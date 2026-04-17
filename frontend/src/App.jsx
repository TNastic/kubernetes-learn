import { useEffect, useMemo, useState } from "react";
import {
  clearToken,
  createTask,
  deleteTask,
  fetchBackendHealth,
  fetchCurrentUser,
  fetchTasks,
  getApiBaseUrl,
  getStoredToken,
  login,
  logout,
  register,
  storeToken,
  updateTask
} from "./api";

const EMPTY_TASK = { title: "", description: "", status: "TODO" };
const STATUS_OPTIONS = ["ALL", "TODO", "DONE"];
const STATUS_LABELS = { ALL: "全部", TODO: "待完成", DONE: "已完成" };

function AuthPanel({ onAuthenticated }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");

  async function submit(event) {
    event.preventDefault();
    setError("");
    try {
      const response = mode === "login" ? await login(form) : await register(form);
      storeToken(response.token);
      onAuthenticated(response.token, response.user);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
    <section className="auth-layout">
      <div className="auth-copy">
        <img
          alt="桌面上的计划本和咖啡"
          src="https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=900&q=80"
        />
        <p className="eyebrow">阶段五 · 业务 MVP</p>
        <h1>把今天要做的事收进一个清爽的工作台。</h1>
        <p>注册、登录、创建任务、筛选状态，数据落到 MySQL，会话放进 Redis。</p>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <h2>{mode === "login" ? "登录" : "注册"}</h2>
        <label>
          用户名
          <input
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
            placeholder="例如 alex"
          />
        </label>
        <label>
          密码
          <input
            type="password"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            placeholder="至少 6 位"
          />
        </label>
        {error && <p className="error-message">{error}</p>}
        <button type="submit">{mode === "login" ? "进入工作台" : "创建账号"}</button>
        <button
          className="plain-button"
          type="button"
          onClick={() => setMode(mode === "login" ? "register" : "login")}
        >
          {mode === "login" ? "没有账号，去注册" : "已有账号，去登录"}
        </button>
      </form>
    </section>
  );
}

function TaskForm({ draft, onChange, onSubmit, onCancel }) {
  return (
    <form className="task-form" onSubmit={onSubmit}>
      <input
        value={draft.title}
        onChange={(event) => onChange({ ...draft, title: event.target.value })}
        placeholder="任务标题"
      />
      <textarea
        value={draft.description}
        onChange={(event) => onChange({ ...draft, description: event.target.value })}
        placeholder="描述一下要完成什么"
      />
      <select
        value={draft.status}
        onChange={(event) => onChange({ ...draft, status: event.target.value })}
      >
        <option value="TODO">待完成</option>
        <option value="DONE">已完成</option>
      </select>
      <div className="form-actions">
        <button type="submit">{draft.id ? "保存修改" : "创建任务"}</button>
        {draft.id && <button className="plain-button" type="button" onClick={onCancel}>取消</button>}
      </div>
    </form>
  );
}

function TaskList({ tasks, onEdit, onDelete, onToggle }) {
  if (!tasks.length) {
    return <p className="empty">这个筛选条件下还没有任务。</p>;
  }

  return (
    <ul className="task-list">
      {tasks.map((task) => (
        <li className="task-card" key={task.id}>
          <div>
            <span className={`status-pill ${task.status.toLowerCase()}`}>
              {STATUS_LABELS[task.status]}
            </span>
            <h3>{task.title}</h3>
            <p>{task.description || "没有描述。"}</p>
          </div>
          <div className="task-actions">
            <button type="button" onClick={() => onToggle(task)}>
              标记为{task.status === "TODO" ? "已完成" : "待完成"}
            </button>
            <button type="button" className="secondary-button" onClick={() => onEdit(task)}>
              编辑
            </button>
            <button type="button" className="danger-button" onClick={() => onDelete(task.id)}>
              删除
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}

function HealthStrip({ apiBaseUrl, health }) {
  return (
    <aside className="health-strip">
      <span>API：{apiBaseUrl}</span>
      <span>后端：{health?.status ?? "检查中"}</span>
      <span>依赖：{health?.dependencies?.map((item) => `${item.name} ${item.status}`).join(" / ")}</span>
    </aside>
  );
}

function Workspace({ token, user, onSignedOut }) {
  const [tasks, setTasks] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [draft, setDraft] = useState(EMPTY_TASK);
  const [health, setHealth] = useState(null);
  const [error, setError] = useState("");
  const apiBaseUrl = useMemo(() => getApiBaseUrl(), []);

  async function loadTasks(nextFilter = filter) {
    setError("");
    try {
      setTasks(await fetchTasks(token, nextFilter));
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function submitTask(event) {
    event.preventDefault();
    const payload = { title: draft.title, description: draft.description, status: draft.status };
    try {
      draft.id ? await updateTask(token, draft.id, payload) : await createTask(token, payload);
      setDraft(EMPTY_TASK);
      await loadTasks();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function signOut() {
    try {
      await logout(token);
      clearToken();
      onSignedOut();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function removeTask(id) {
    try {
      await deleteTask(token, id);
      await loadTasks();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function toggleTask(task) {
    const nextStatus = task.status === "TODO" ? "DONE" : "TODO";
    try {
      await updateTask(token, task.id, { ...task, status: nextStatus });
      await loadTasks();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  useEffect(() => {
    loadTasks();
    fetchBackendHealth().then(setHealth).catch((requestError) => setError(requestError.message));
  }, []);

  return (
    <main className="workspace">
      <header className="topbar">
        <div>
          <p className="eyebrow">个人任务管理</p>
          <h1>{user.username} 的任务工作台</h1>
        </div>
        <button className="secondary-button" type="button" onClick={signOut}>退出登录</button>
      </header>
      <HealthStrip apiBaseUrl={apiBaseUrl} health={health} />
      <section className="board">
        <TaskForm
          draft={draft}
          onChange={setDraft}
          onSubmit={submitTask}
          onCancel={() => setDraft(EMPTY_TASK)}
        />
        <div className="filters">
          {STATUS_OPTIONS.map((status) => (
            <button
              className={filter === status ? "active-filter" : "plain-button"}
              key={status}
              type="button"
              onClick={() => {
                setFilter(status);
                loadTasks(status);
              }}
            >
              {STATUS_LABELS[status]}
            </button>
          ))}
        </div>
        {error && <p className="error-message">{error}</p>}
        <TaskList
          tasks={tasks}
          onEdit={setDraft}
          onDelete={removeTask}
          onToggle={toggleTask}
        />
      </section>
    </main>
  );
}

export default function App() {
  const [token, setToken] = useState(getStoredToken());
  const [user, setUser] = useState(null);
  const [bootError, setBootError] = useState("");

  useEffect(() => {
    if (!token) {
      return;
    }
    fetchCurrentUser(token).then(setUser).catch((error) => {
      clearToken();
      setToken(null);
      setBootError(error.message);
    });
  }, [token]);

  if (!token || !user) {
    return (
      <>
        {bootError && <p className="boot-error">{bootError}</p>}
        <AuthPanel onAuthenticated={(nextToken, nextUser) => {
          setToken(nextToken);
          setUser(nextUser);
        }} />
      </>
    );
  }

  return <Workspace token={token} user={user} onSignedOut={() => {
    setToken(null);
    setUser(null);
  }} />;
}

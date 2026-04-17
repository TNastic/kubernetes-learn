import { useEffect, useMemo, useState } from "react";
import { fetchBackendHealth, getApiBaseUrl } from "./api";

const REQUEST_STATE = {
  LOADING: "loading",
  READY: "ready",
  ERROR: "error"
};

function formatCheckedAt(value) {
  if (!value) {
    return "尚未返回";
  }

  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(value));
}

function DependencyList({ dependencies }) {
  if (!dependencies.length) {
    return <p className="empty">后端暂未返回依赖状态。</p>;
  }

  return (
    <ul className="dependency-list">
      {dependencies.map((dependency) => (
        <li className="dependency-item" key={dependency.name}>
          <span className="dependency-name">{dependency.name}</span>
          <span className={`badge ${dependency.status.toLowerCase()}`}>
            {dependency.status}
          </span>
          <span className="dependency-detail">{dependency.detail}</span>
        </li>
      ))}
    </ul>
  );
}

function PageHeader() {
  return (
    <>
      <p className="eyebrow">阶段 4 · 前端最小闭环</p>
      <h1 id="page-title">后端健康检查</h1>
      <p className="intro">
        当前前端通过环境变量访问后端 API，并显示 MySQL 与 Redis 的连通状态。
      </p>
    </>
  );
}

function HealthSummary({ apiBaseUrl, health }) {
  const isHealthy = health?.status === "UP";

  return (
    <div className="summary">
      <div>
        <span className="label">API 入口</span>
        <strong>{apiBaseUrl}</strong>
      </div>
      <div>
        <span className="label">后端状态</span>
        <strong className={isHealthy ? "healthy" : "unhealthy"}>
          {health?.status ?? "检查中"}
        </strong>
      </div>
      <div>
        <span className="label">检查时间</span>
        <strong>{formatCheckedAt(health?.checkedAt)}</strong>
      </div>
    </div>
  );
}

function RequestFeedback({ requestState, errorMessage }) {
  if (requestState === REQUEST_STATE.ERROR) {
    return <p className="error-message">请求失败：{errorMessage}</p>;
  }

  if (requestState === REQUEST_STATE.LOADING) {
    return <p className="loading-message">正在连接后端健康检查接口...</p>;
  }

  return null;
}

export default function App() {
  const [requestState, setRequestState] = useState(REQUEST_STATE.LOADING);
  const [health, setHealth] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const apiBaseUrl = useMemo(() => getApiBaseUrl(), []);

  async function loadHealth() {
    setRequestState(REQUEST_STATE.LOADING);
    setErrorMessage("");

    try {
      setHealth(await fetchBackendHealth());
      setRequestState(REQUEST_STATE.READY);
    } catch (error) {
      setRequestState(REQUEST_STATE.ERROR);
      setErrorMessage(error.message);
    }
  }

  useEffect(() => {
    loadHealth();
  }, []);

  return (
    <main className="page">
      <section className="status-panel" aria-labelledby="page-title">
        <PageHeader />
        <HealthSummary apiBaseUrl={apiBaseUrl} health={health} />
        <RequestFeedback
          requestState={requestState}
          errorMessage={errorMessage}
        />

        {requestState !== REQUEST_STATE.LOADING && (
          <DependencyList dependencies={health?.dependencies ?? []} />
        )}

        <button type="button" onClick={loadHealth}>
          重新检查
        </button>
      </section>
    </main>
  );
}

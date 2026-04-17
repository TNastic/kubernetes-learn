package com.example.taskmanager.mapper;

import com.example.taskmanager.entity.TaskItem;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TaskMapper {

    @Select("<script>"
            + "SELECT id, user_id, title, description, status, created_at, updated_at FROM tasks "
            + "WHERE user_id = #{userId} "
            + "<if test='status != null'>AND status = #{status} </if>"
            + "ORDER BY updated_at DESC, id DESC"
            + "</script>")
    List<TaskItem> findByUser(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT id, user_id, title, description, status, created_at, updated_at "
            + "FROM tasks WHERE id = #{id} AND user_id = #{userId}")
    TaskItem findOne(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("INSERT INTO tasks (user_id, title, description, status) "
            + "VALUES (#{userId}, #{title}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskItem task);

    @Update("UPDATE tasks SET title = #{title}, description = #{description}, status = #{status} "
            + "WHERE id = #{id} AND user_id = #{userId}")
    int update(TaskItem task);

    @Delete("DELETE FROM tasks WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);
}

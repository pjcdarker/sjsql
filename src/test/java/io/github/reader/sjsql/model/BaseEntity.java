package io.github.reader.sjsql.model;

import java.time.LocalDateTime;

public class BaseEntity {

    protected Long id;
    protected LocalDateTime createTime;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

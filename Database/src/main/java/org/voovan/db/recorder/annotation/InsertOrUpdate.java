package org.voovan.db.recorder.annotation;

import java.lang.annotation.*;

/**
 * Recorder 对象的 InsertOrUpdate 注解
 * Field 增加这个注解可标记在 insert 操作时的存在即更新的行为
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InsertOrUpdate {
}

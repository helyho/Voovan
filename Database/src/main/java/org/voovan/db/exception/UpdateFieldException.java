package org.voovan.db.exception;

import java.sql.SQLException;

/**
 * 更新异常
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class UpdateFieldException extends RuntimeException {

    private static final long	serialVersionUID	= 1L;

    public UpdateFieldException(String message, Exception e){
        super(message);
        this.setStackTrace(e.getStackTrace());
    }

    public UpdateFieldException(String message){
        super(message);
    }

    public UpdateFieldException(Exception e){
        this.setStackTrace(e.getStackTrace());
    }
}

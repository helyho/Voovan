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
public class UpdateCountException extends SQLException {

    private static final long	serialVersionUID	= 1L;

    public UpdateCountException(String message, Exception e){
        super(message);
        this.setStackTrace(e.getStackTrace());
    }

    public UpdateCountException(String message){
        super(message);
    }

    public UpdateCountException(Exception e){
        this.setStackTrace(e.getStackTrace());
    }


}

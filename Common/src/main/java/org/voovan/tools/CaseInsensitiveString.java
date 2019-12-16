package org.voovan.tools;

/**
 * 大小写不敏感的字符串类
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class CaseInsensitiveString {
    private String s;

    public CaseInsensitiveString(String s) {
        if (s == null)
            throw new NullPointerException();
        this.s = s;
    }

    public boolean equals(Object o) {
        return o instanceof CaseInsensitiveString &&
                ((CaseInsensitiveString)o).s.equalsIgnoreCase(s);
    }

    private volatile int hashCode = 0;

    public int hashCode() {
        if (hashCode == 0)
            hashCode = s.toUpperCase().hashCode();

        return hashCode;
    }

    public String toString() {
        return s;
    }
}

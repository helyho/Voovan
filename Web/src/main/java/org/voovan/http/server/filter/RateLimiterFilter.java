package org.voovan.http.server.filter;

import org.voovan.http.server.HttpFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.tools.collection.MultiMap;
import org.voovan.tools.bucket.Bucket;
import org.voovan.tools.collection.CachedHashMap;
import org.voovan.tools.bucket.LeakBucket;
import org.voovan.tools.bucket.TokenBucket;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;


public class RateLimiterFilter implements HttpFilter {

    private static MultiMap<String, Limiter> LIMITER_DEFINE_MAP = new MultiMap<String, Limiter>();

    private static Function<Limiter, Object> bucketRelease = (limiter) -> {
        limiter.getBucket().release();
        return null;
    };

    private static CachedHashMap<String, Limiter> URL_LIMITER_MAP = new CachedHashMap<String,Limiter>(10000).autoRemove(true).interval(1).destory(bucketRelease).create();
    private static CachedHashMap<String, Limiter> IP_LIMITER_MAP = new CachedHashMap<String,Limiter>(10000).autoRemove(true).interval(1).destory(bucketRelease).create();
    private static CachedHashMap<String, Limiter> HEADER_LIMITER_MAP = new CachedHashMap<String,Limiter>(10000).autoRemove(true).interval(1).destory(bucketRelease).create();
    private static CachedHashMap<String, Limiter> SESSION_LIMITER_MAP = new CachedHashMap<String,Limiter>(10000).autoRemove(true).interval(1).destory(bucketRelease).create();

    private AtomicBoolean isInit = new AtomicBoolean(false);

    @Override
    public Object onRequest(HttpFilterConfig httpFilterConfig, HttpRequest httpRequest, HttpResponse httpResponse, Object o) {
        List<Map> limiterMapList = (List<Map>) httpFilterConfig.getParameter("limiter");

        if(isInit.compareAndSet(false, true)) {
            try {
                for (Map limiterMap : limiterMapList) {
                    Limiter limiter = ((Limiter) TReflect.getObjectFromMap(Limiter.class, limiterMap, true));
                    LIMITER_DEFINE_MAP.putValue(limiter.type.toUpperCase(), limiter);
                }
            } catch (Exception e) {
                Logger.error("TokenBucketFilter error: ", e);
            }
        }

        //-------------------url---------------------
        String requestPath = httpRequest.protocol().getPath();
        Limiter urlLimiter = URL_LIMITER_MAP.get(requestPath);
        if(urlLimiter == null) {
            List urlLimiterList = LIMITER_DEFINE_MAP.getValues("URL");
            if(urlLimiterList!=null) {
                for (Limiter limiterDefine : LIMITER_DEFINE_MAP.getValues("URL")) {
                    if (limiterDefine.getValue().equals(requestPath)) {
                        urlLimiter = limiterDefine.newInstance();
                        URL_LIMITER_MAP.put(urlLimiter.getValue(), urlLimiter);
                    }
                }
            }
        }
        if(dealLimiter(urlLimiter, httpResponse)){
            return null;
        }

        //-------------------ip---------------------
        String ipAddress = httpRequest.getRemoteAddres();
        Limiter ipLimiter = IP_LIMITER_MAP.get(ipAddress);
        if(ipLimiter == null) {
            List ipLimiterList = LIMITER_DEFINE_MAP.getValues("IP");
            if(ipLimiterList!=null) {
                for (Limiter limiterDefine : LIMITER_DEFINE_MAP.getValues("IP")) {
                    if (limiterDefine.getValue().equals(ipAddress)) {
                        ipLimiter = limiterDefine.newInstance();
                        IP_LIMITER_MAP.put(ipLimiter.getValue(), ipLimiter);
                    }
                }
            }
        }
        if(dealLimiter(ipLimiter, httpResponse)){
            return null;
        }

        //-------------------header---------------------

        Limiter headerLimiter = null;
        List headerLimiterList = LIMITER_DEFINE_MAP.getValues("HEADER");
        if(headerLimiterList!=null) {
            for (Limiter limiterDefine : LIMITER_DEFINE_MAP.getValues("HEADER")) {
                String headerValue = httpRequest.header().get(limiterDefine.getValue());

                if(headerValue == null){
                    continue;
                }

                headerLimiter = HEADER_LIMITER_MAP.get(headerValue);
                if (headerLimiter != null) {
                    if(dealLimiter(headerLimiter, httpResponse)){
                        return null;
                    }
                } else {
                    headerLimiter = limiterDefine.newInstance();
                    HEADER_LIMITER_MAP.put(headerValue, headerLimiter);
                }
            }
        }



        //-------------------session---------------------

        Limiter sessionLimiter = null;
        List sessionLimiterList = LIMITER_DEFINE_MAP.getValues("SESSION");
        if(sessionLimiterList!=null) {
            for (Limiter limiterDefine : LIMITER_DEFINE_MAP.getValues("SESSION")) {
                String sessionValue = httpRequest.getSession().getAttribute(limiterDefine.getValue()).toString();

                if(sessionValue == null){
                    continue;
                }

                sessionLimiter = SESSION_LIMITER_MAP.get(sessionValue);
                if(sessionLimiter!=null) {
                    if(dealLimiter(sessionLimiter, httpResponse)){
                        return null;
                    }
                } else {
                    sessionLimiter = limiterDefine.newInstance();
                    SESSION_LIMITER_MAP.put(sessionValue, sessionLimiter);
                }
            }
        }


        return true;
    }

    public boolean dealLimiter(Limiter limiter, HttpResponse httpResponse){
        if (limiter != null) {
            if (limiter.getBucket().acquire()) {
                return false;
            } else {
                httpResponse.write(limiter.getResponse());
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public Object onResponse(HttpFilterConfig httpFilterConfig, HttpRequest httpRequest, HttpResponse httpResponse, Object o) {
        return true;
    }

    public class Limiter {
        private String value;
        private String type;
        private String response;
        private int limitSize;
        private int interval;
        private String bucketType;

        private Bucket bucket;

        private Limiter() {

        }

        public Limiter init(){
            if ("LEAK".equalsIgnoreCase(bucketType)){
                bucket = new LeakBucket(limitSize, interval);
            } else {
                bucket = new TokenBucket(limitSize, interval);
            }
            return this;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public int getLimitSize() {
            return limitSize;
        }

        public void setLimitSize(int limitSize) {
            this.limitSize = limitSize;
        }

        public int getInterval() {
            return interval;
        }

        public void setInterval(int interval) {
            this.interval = interval;
        }

        public Bucket getBucket() {
            return bucket;
        }

        public void setBucket(Bucket bucket) {
            this.bucket = bucket;
        }

        public String getBucketType() {
            return bucketType;
        }

        public void setBucketType(String bucketType) {
            this.bucketType = bucketType;
        }

        public Limiter newInstance(){
            Limiter limiter = new Limiter();
            limiter.setLimitSize(this.getLimitSize());
            limiter.setInterval(this.interval);
            limiter.setResponse(this.response);
            limiter.setType(this.type);
            limiter.setValue(this.value);
            limiter.setBucketType(bucketType);

            return limiter.init();
        }
    }

}

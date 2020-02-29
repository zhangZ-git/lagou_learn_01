package com.lagou.sqlSession;

import com.lagou.pojo.Configuration;
import com.lagou.pojo.MappedStatement;

import java.lang.reflect.*;
import java.util.List;

public class DefaultSqlSession implements SqlSession {

    private Configuration configuration;

    public DefaultSqlSession(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <E> List<E> selectList(String statementid, Object... params) throws Exception {

        //将要去完成对simpleExecutor里的query方法的调用
        simpleExecutor simpleExecutor = new simpleExecutor();
        MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementid);
        List<Object> list = simpleExecutor.query(configuration,mappedStatement, params);

        return (List<E>) list;
    }

    @Override
    public <T> T selectOne(String statementid, Object... params) throws Exception {
        List<Object> objects = selectList(statementid, params);
        if(objects.size()==1){
            return (T) objects.get(0);
        }else {
            throw new RuntimeException("查询结果为空或者返回结果过多");
        }


    }

    @Override
    public <T> int updateDbInfo(String statementid, Object... params) throws Exception {
        simpleExecutor simpleExecutor = new simpleExecutor();
        MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementid);
        int update = simpleExecutor.updateDelIn(configuration, mappedStatement, params);
        return update;
    }

    @Override
    public <T> int deleteDbInfo(String statementid, Object... params) throws Exception {
        simpleExecutor simpleExecutor = new simpleExecutor();
        MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementid);
        int del = simpleExecutor.updateDelIn(configuration, mappedStatement, params);
        return del;
    }

    public <T> int insertDbInfo(String statementid, Object... params) throws Exception {
        simpleExecutor simpleExecutor = new simpleExecutor();
        MappedStatement mappedStatement = configuration.getMappedStatementMap().get(statementid);
        int ins = simpleExecutor.updateDelIn(configuration, mappedStatement, params);
        return ins;
    }


    @Override
    public <T> T getMapper(Class<?> mapperClass) {
        // 使用JDK动态代理来为Dao接口生成代理对象，并返回

        Object proxyInstance = Proxy.newProxyInstance(DefaultSqlSession.class.getClassLoader(), new Class[]{mapperClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 底层都还是去执行JDBC代码 //根据不同情况，来调用selctList或者selectOne
                // 准备参数 1：statmentid :sql语句的唯一标识：namespace.id= 接口全限定名.方法名
                // 方法名：findAll
                String methodName = method.getName();
                String className = method.getDeclaringClass().getName();

                String statementId = className + "." + methodName;
                String sqlType = configuration.getMappedStatementMap().get(statementId).getChoseElement();
                if (sqlType == "select") {
                    // 准备参数2：params:args
                    // 获取被调用方法的返回值类型
                    Type genericReturnType = method.getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType) {
                        List<Object> objects = selectList(statementId, args);
                        return objects;
                    }
                    return selectOne(statementId, args);
                }else if(sqlType=="update"){
                    return updateDbInfo(statementId,args);
                }else if(sqlType=="delete"){
                    return deleteDbInfo(statementId,args);
                }else if(sqlType=="insert"){
                    return insertDbInfo(statementId,args);
                }
                return null;
            }
        });

        return (T) proxyInstance;
    }



}

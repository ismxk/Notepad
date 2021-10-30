package com.noobyang.database;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * partial quote
 */

public class OrmLiteDao<T> {

    private static final String TAG = "OrmLiteDao";

    protected final Dao<T, Integer> ormLiteDao;

    protected final DatabaseHelper helper;

    public interface DaoOperation {

        /**
         * 插入
         */
        int INSERT = 1;
        /**
         * 删除
         */
        int DELETE = 2;
        /**
         * 更新
         */
        int UPDATE = 3;
        /**
         * 查询
         */
        int SELECT = 4;
        /**
         * 批量插入
         */
        int INSERT_BATCH = 5;
        /**
         * 批量删除
         */
        int DELETE_BATCH = 6;
        /**
         * 批量更新
         */
        int UPDATE_BATCH = 7;
    }


    public OrmLiteDao(Context context, Class<T> cls, String dbName) {
        helper = DatabaseHelper.getInstance(context.getApplicationContext(), dbName);
        ormLiteDao = helper.getDao(cls);
    }

    /**
     * 获取指定类的数据访问对象
     */
    public Dao<T, Integer> getDao() {
        return ormLiteDao;
    }

    /**
     * 数据批量处理
     *
     * @param list      要处理的数据集合
     * @param batchType 操作类型
     */
    private boolean doBatchInTransaction(final List<T> list, final int batchType) {
        boolean doBatch = false;
        ConnectionSource connectionSource = ormLiteDao.getConnectionSource();
        TransactionManager transactionManager = new TransactionManager(connectionSource);
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doBatch(list, batchType);
            }
        };
        try {
            doBatch = transactionManager.callInTransaction(callable);
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return doBatch;
    }

    /**
     * 数据批量处理的实现
     *
     * @param list      要处理的数据集合
     * @param batchType 操作类型
     */
    private boolean doBatch(List<T> list, int batchType) {
        int result = 0;
        try {
            for (T t : list) {
                switch (batchType) {
                    case DaoOperation.INSERT:
                        result += ormLiteDao.create(t);
                        break;
                    case DaoOperation.DELETE:
                        result += ormLiteDao.delete(t);
                        break;
                    case DaoOperation.UPDATE:
                        result += updateIfValueNotNull(t);
                        break;
                    default:
                        Log.w(TAG, "no this type.");
                        break;
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return result == list.size();
    }

    /**
     * 增加一条记录
     *
     * @param t 新增数据实体
     */
    public boolean insert(T t) {
        int result = 0;
        try {
            result = ormLiteDao.create(t);
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return result > 0;
    }

    /**
     * 批量插入
     *
     * @param list 插入的数据集合
     */
    public boolean insertForBatch(List<T> list) {
        return doBatchInTransaction(list, DaoOperation.INSERT);
    }

    /**
     * 清空表数据
     *

     */
    public boolean clearTableData() {
        long count = 0;
        try {
            count = ormLiteDao.countOf();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        if (count == 0) {
            return true;
        }
        int result = 0;
        DeleteBuilder deleteBuilder = ormLiteDao.deleteBuilder();
        try {
            result = deleteBuilder.delete();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
            return false;
        }
        return result > 0;
    }

    /**
     * 按条件删除
     *
     * @param columnName 指定删除条件列名
     * @param value      删除条件对应的值
     */
    public boolean deleteByColumnName(String columnName, Object value) {
        int result = 0;
        DeleteBuilder deleteBuilder = ormLiteDao.deleteBuilder();
        try {
            deleteBuilder.where().eq(columnName, value);
            result = deleteBuilder.delete();
        } catch (SQLException e) {
            Log.e(TAG, "delete error, columnName: " + columnName + ", value: " + value + ", result: " + result);
            return false;
        }
        return result > 0;
    }

    /**
     * 通过表列名来删除
     *
     * @param map key是列名,value是列对应的值
     */
    public boolean deleteByColumnName(Map<String, Object> map) {
        int result = 0;
        DeleteBuilder deleteBuilder = ormLiteDao.deleteBuilder();
        Where where = deleteBuilder.where();
        try {
            boolean isFirst = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if(isFirst){
                    where.eq(entry.getKey(), entry.getValue());
                    isFirst = false;
                }else{
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            result = deleteBuilder.delete();
        } catch (SQLException e) {
            Log.e("delete error,delete line:" + result, e.toString());
            return false;
        }
        return result > 0;
    }

    /**
     * 删除小于指定列的值的所有数据
     *
     * @param columnName 指定列名
     * @param value      指定数值，小于该值的数据将被删除
     */
    public int deleteLtValue(String columnName, Object value) {
        int result = 0;
        DeleteBuilder deleteBuilder = ormLiteDao.deleteBuilder();
        try {
            deleteBuilder.where().lt(columnName, value);
            result = deleteBuilder.delete();
        } catch (SQLException e) {
            Log.e(TAG, "delete error, columnName: " + columnName + ", value: " + value + ", result: " + result);
            return result;
        }
        return result;
    }

    /**
     * 清空|删除表所有数据
     */
    public int deleteAll() {
        int result = 0;
        DeleteBuilder deleteBuilder = ormLiteDao.deleteBuilder();
        try {
            result = deleteBuilder.delete();
        } catch (SQLException e) {
            Log.e("clear table data error ", e.toString());
            return result;
        }
        return result;
    }

    /**
     * 批量删除，list中的item必须有id
     *
     * @param list 要删除的记录集合
     */
    public boolean deleteForBatch(List<T> list) {
        return doBatchInTransaction(list, DaoOperation.DELETE);
    }

    /**
     * 获取满足指定条件的记录数
     *
     * @param map 查询条件键值组合
     */
    public long getCount(Map<String, Object> map) {
        long count = 0;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        queryBuilder.setCountOf(true);
        Where where = queryBuilder.where();
        try {
            boolean isFirst = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if(isFirst){
                    where.eq(entry.getKey(), entry.getValue());
                    isFirst = false;
                }else {
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            PreparedQuery<T> preparedQuery = queryBuilder.prepare();
            count = ormLiteDao.countOf(preparedQuery);
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return count;
    }

    public long getCount() {
        long count = 0;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        queryBuilder.setCountOf(true);
        try {
            PreparedQuery<T> preparedQuery = queryBuilder.prepare();
            count = ormLiteDao.countOf(preparedQuery);
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return count;
    }

    /**
     * 查询全部记录
     */
    public List<T> queryForAll() {
        List<T> list = null;
        try {
            list = ormLiteDao.queryForAll();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 根据表中任意字段进行查询
     *
     * @param map 查询条件键值组合
     */
    public List<T> queryByColumnName(Map<String, Object> map) {
        List<T> list = null;
        try {
            list = ormLiteDao.queryForFieldValuesArgs(map);
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 通过表列名查询
     *
     * @param columnName 指定查询条件列名
     * @param value      查询条件值
     */
    public List<T> queryByColumnName(String columnName, Object value) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        try {
            queryBuilder.where().eq(columnName, value);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 返回数据库中所有记录的指定列的值
     *
     * @param selectColumns 指定列名
     */
    public List<T> queryAllBySelectColumns(String[] selectColumns) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        try {
            queryBuilder.selectColumns(selectColumns);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 查询大于某个值的记录
     *
     * @param orderColumn 排序的列
     * @param value       某个值
     */
    public List<T> queryAllByGt(String orderColumn, Object value) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.gt(orderColumn, value);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 排序查询
     *
     * @param orderColumn 排序的列
     * @param ascending   true为升序,false为降序
     */
    public List<T> queryAllByOrder(String orderColumn, boolean ascending) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.isNotNull(orderColumn);
            queryBuilder.orderBy(orderColumn, ascending);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 排序查询
     *
     * @param orderColumn 排序的列
     * @param columnName  指定查询条件列名
     * @param value       查询条件值
     * @param ascending   true为升序,false为降序
     */
    public List<T> queryByOrder(String orderColumn, String columnName, Object value, boolean ascending) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.eq(columnName, value);
            queryBuilder.orderBy(orderColumn, ascending);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 排序查询大于指定值的所有记录
     * 之前 orderColumn >= limitValue && columnName = value
     * 改为 orderColumn >= limitValue
     * @param orderColumn 大于的列
     * @param limitValue  大于等于的值
     * @param ascending   true为升序,false为降序
     */
    public List<T> queryGeByOrder(String orderColumn, Object limitValue, boolean ascending) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.ge(orderColumn, limitValue);
            queryBuilder.orderBy(orderColumn, ascending);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 排序查询小于指定值的所有记录
     *
     * @param orderColumn 小于的列
     * @param limitValue  小于等于的值
     * @param ascending   true为升序,false为降序
     */
    public List<T> queryLeByOrder(String orderColumn, Object limitValue, boolean ascending) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.le(orderColumn, limitValue);
            queryBuilder.orderBy(orderColumn, ascending);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 分页查询,并按列排序
     *
     * @param orderColumn 排序列名
     * @param ascending   true为升序,false为降序
     * @param offset      搜索下标
     * @param count       搜索条数
     */
    public List<T> queryForPagesByOrder(String orderColumn, boolean ascending, Long offset, Long count) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.isNotNull(orderColumn);
            queryBuilder.orderBy(orderColumn, ascending);
            queryBuilder.offset(offset);
            queryBuilder.limit(count);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 分页查询,并按列排序
     *
     * @param columnName  查询条件列名
     * @param value       查询条件值
     * @param orderColumn 排序列名
     * @param ascending   true为升序,false为降序
     * @param offset      搜索下标
     * @param count       搜索条数
     */
    public List<T> queryForPagesByOrder(String columnName, Object value, String orderColumn, boolean ascending, Long offset, Long count) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.eq(columnName, value);
            queryBuilder.orderBy(orderColumn, ascending);
            queryBuilder.offset(offset);
            queryBuilder.limit(count);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 分页查询,并按列排序
     *
     * @param map         查询的列条件
     * @param offset      查询的下标
     * @param count       查询的条数
     * @param orderColumn 排序的列
     * @param ascending   升序或降序,true为升序,false为降序
     */
    public List<T> queryForPagesByOrder(Map<String, Object> map, String orderColumn, boolean ascending, Long offset, Long count) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            queryBuilder.orderBy(orderColumn, ascending);
            queryBuilder.offset(offset);
            queryBuilder.limit(count);
            boolean isFirst = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if(isFirst){
                    where.eq(entry.getKey(), entry.getValue());
                    isFirst = false;
                }else{
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 按条件查询，返回第一条符号要求的记录
     *
     * @param columnName 查询条件列名
     * @param value      查询条件值
     */
    public T queryForFirst(String columnName, Object value) {
        T t = null;
        QueryBuilder<T, Integer> queryBuilder = ormLiteDao.queryBuilder();
        try {
            queryBuilder.where().eq(columnName, value);
            t = queryBuilder.queryForFirst();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return t;
    }

    /**
     * 通过表列名查询第一条记录
     *
     * @param map 查询条件键值组合
     */
    public T queryForFirst(Map<String, Object> map) {
        T t = null;
        QueryBuilder<T, Integer> queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            boolean isFirst = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if(isFirst){
                    where.eq(entry.getKey(), entry.getValue());
                    isFirst = false;
                }else{
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            t = queryBuilder.queryForFirst();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return t;
    }

    /**
     * 查询数据库第一个，当列数据不为空时的数据
     *
     * @param columnName 列名
     */
    public T queryForFirstColumnNotNull(String columnName) {
        T t = null;
        QueryBuilder<T, Integer> queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.isNotNull(columnName);
            t = queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return t;
    }

    public List<T> queryAllColumnNotNull(String columnName) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.isNotNull(columnName);
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 排序查询
     *
     * @param map         查询条件键值组合
     * @param orderColumn 排序的列
     * @param ascending   是否升序
     */
    public T queryForFirstByOrder(Map<String, Object> map, String orderColumn, boolean ascending) {
        T t = null;
        QueryBuilder<T, Integer> queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            queryBuilder.orderBy(orderColumn, ascending);
            boolean isFirst = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if(isFirst){
                    where.eq(entry.getKey(), entry.getValue());
                    isFirst = false;
                }else{
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            t = queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return t;
    }

    /**
     * 排序查询
     *
     * @param columnName  查询条件列名
     * @param value       查询条件值
     * @param orderColumn 排序的列
     * @param ascending   是否升序
     */
    public T queryForFirstByOrder(String columnName, Object value, String orderColumn, boolean ascending) {
        T t = null;
        QueryBuilder<T, Integer> queryBuilder = ormLiteDao.queryBuilder();
        try {
            queryBuilder.orderBy(orderColumn, ascending);
            queryBuilder.where().eq(columnName, value);
            t = queryBuilder.queryForFirst();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return t;
    }

    /**
     * 查询列名不等于指定值的记录
     *
     * @param columnName 列名
     * @param value      指定值
     */
    public List<T> queryNotEqualsByColumnName(String columnName, Object value) {
        List<T> list = null;
        QueryBuilder queryBuilder = ormLiteDao.queryBuilder();
        Where where = queryBuilder.where();
        try {
            where.or(where.gt(columnName, value), where.lt(columnName, value));
            list = queryBuilder.query();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return list;
    }

    /**
     * 修改记录，ID不能为空
     *
     * @param t 新的数据实体
     */
    public boolean update(T t) {
        int result = updateIfValueNotNull(t);
        return result > 0;
    }

    public boolean updateBy(T t, String columnName, Object value) {
        int result = 0;
        T t1 = queryForFirst(columnName, value);
        if (t1 == null) {
            Log.e(TAG, "no find this data in database:" + t);
            return false;
        }
        try {
            setObjectValueIfNotNull(t1, t);
            result = ormLiteDao.update(t1);
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.toString());
        } catch (NoSuchFieldException e) {
            Log.e(TAG, e.toString());
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return result > 0;
    }

    /**
     * 按条件更新
     *
     * @param t     新的数据实体
     * @param value 更新条件
     */
    public boolean updateBy(T t, Map<String, Object> value) {
        int result = 0;
        T t1 = queryForFirst(value);
        if (t1 == null) {
            Log.e(TAG, "no find this data in database:" + t);
            return false;
        }
        try {
            setObjectValueIfNotNull(t1, t);
            result = ormLiteDao.update(t1);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "update error,update line:" + result);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "update error,update line:" + result);
        } catch (SQLException e) {
            Log.e(TAG, "update error,update line:" + result);
        }
        return result > 0;
    }

    /**
     * 批量修改
     *
     * @param list 记录集合
     */
    public boolean updateForBatch(List<T> list) {
        return doBatchInTransaction(list, DaoOperation.UPDATE);
    }

    /**
     * 更新
     * 如果更新记录字段值为null则忽略不更新
     */
    private int updateIfValueNotNull(T t) {
        int result = 0;
        UpdateBuilder updateBuilder = ormLiteDao.updateBuilder();
        Map<String, Object> map = getFieldsIfValueNotNull(t);
        if (map.isEmpty()) {
            Log.w(TAG, "all field value is null.");
            return 0;
        }

        String primaryKey = getDbPrimarykey(t);
        if (map.get(primaryKey) == null) {
            Log.w(TAG, primaryKey +" is null.");
            return 0;
        }

        try {
            updateBuilder.where().idEq(map.get(primaryKey));
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Log.d(TAG,"this key ==" + entry.getKey() + ",this value = " + entry.getValue());
                if (entry.getKey().equals(primaryKey)) {
                    continue;
                }
                updateBuilder.updateColumnValue(entry.getKey(), entry.getValue());
            }
            result = updateBuilder.update();
        } catch (SQLException e) {
            Log.e(TAG, e.toString());
        }
        return result;
    }

    /**
     * 过滤数据实体中值为null的字段
     *
     * @param t   过滤后不包含null字段的数据
     * @param obj 被过滤的数据
     */
    private void setObjectValueIfNotNull(T t, Object obj) throws IllegalAccessException, NoSuchFieldException {
        Field[] fields = obj.getClass().getDeclaredFields();
        Class<?> cls = t.getClass();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals(getDbPrimarykey(obj))) {
                continue;
            }
            Object valueObj = field.get(obj);
            if (valueObj != null) {
                Field f = cls.getDeclaredField(field.getName());
                if (f != null) {
                    f.setAccessible(true);
                    f.set(t, valueObj);
                } else {
                    Log.e(TAG, "no this field:" + field.getName());
                }
            }
        }
    }

    /**
     * 获取对象属性值不为空的属性名称和属性值
     *
     * @param obj 数据实体对象
     */
    private Map<String, Object> getFieldsIfValueNotNull(Object obj) {
        Map<String, Object> map = new HashMap<String, Object>();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            boolean fieldHasAnno = field.isAnnotationPresent(DatabaseField.class);
            //该字段是否有数据库DatabaseField注解
            if(fieldHasAnno) {
                Object valueObj = null;
                try {
                    valueObj = field.get(obj);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.toString());
                }
                if (valueObj != null) {
                    map.put(field.getName(), valueObj);
                }
            }
        }
        return map;
    }

    /**
     * 检测自增id是否为空
     *
     * @param id id
     */
    private boolean checkIdIsNull(Integer id) {
        if (id == null) {
            Log.w(TAG, "id is null.");
            return true;
        }
        return false;
    }

    //获取数据库表封装的bean的主键
    private String getDbPrimarykey(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        String primaryKey = "";
        for (Field field : fields) {
            String name = field.getName();
            //获取属性上的指定类型的注解
            Annotation annotation = field.getAnnotation(DatabaseField.class);
            if (null != annotation) {
                DatabaseField databaseField = (DatabaseField) annotation;
                //通过注解判断是否是主键
                if (databaseField.generatedId() || databaseField.id() || !TextUtils.isEmpty(databaseField.generatedIdSequence())) {
                    primaryKey = name;
                    break;
                }
            }
        }
        if(TextUtils.isEmpty(primaryKey)){
            primaryKey = "id";
            Log.e(TAG,"这里获取的主键信息居然为空,那我就匹配主键为id来容错");
        }
        return primaryKey;
    }

    public String getDbAnnotationField(Object object){
        String result = "";
        Field[] fields =  object.getClass().getDeclaredFields();
        for(Field field : fields){
            boolean fieldHasAnno = field.isAnnotationPresent(DatabaseField.class);
            if(fieldHasAnno) {
                DatabaseField fieldAnno = field.getAnnotation(DatabaseField.class);
                result = field.getName();
            }
        }
        return result;
    }
}

package com.qwli7.blog.entity.dto;

import com.qwli7.blog.entity.vo.AbstractQueryParam;

import java.io.Serializable;
import java.util.List;

/**
 * @author qwli7
 * 2021/3/2 10:15
 * 功能：PageDto
 **/
public class PageDto<T> implements Serializable {

    /**
     * 查询参数，用以回显
     */
    private AbstractQueryParam queryParam;

    /**
     * 当前页数
     */
    private Integer page;

    /**
     * 页大小
     */
    private Integer size;

    /**
     * 总记录数
     */
    private Integer totalRows;

    /**
     * 总页数
     */
    private Integer totalPage;

    /**
     * 数据集合
     */
    private List<T> data;

    public PageDto(AbstractQueryParam queryParam, Integer totalRows, List<T> data) {
        this.queryParam = queryParam;
        this.totalRows = totalRows;
        this.page = queryParam.getPage();
        this.size = queryParam.getSize();
        this.totalPage = (getTotalRows()%getSize() == 0) ? getTotalRows()/getSize() : (getTotalRows()/getSize())+1;
        this.data = data;
    }

    public AbstractQueryParam getQueryParam() {
        return queryParam;
    }

    public void setQueryParam(AbstractQueryParam queryParam) {
        this.queryParam = queryParam;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}

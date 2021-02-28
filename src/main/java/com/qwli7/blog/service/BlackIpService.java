package com.qwli7.blog.service;

import com.qwli7.blog.entity.BlackIp;

import java.util.List;

/**
 * @author qwli7
 * @date 2021/2/22 13:09
 * 功能：blog8
 **/
public interface BlackIpService {
    List<BlackIp> selectAll();


    void save(BlackIp blackIp);

    void delete(int id);
}

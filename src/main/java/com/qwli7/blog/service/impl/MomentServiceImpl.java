package com.qwli7.blog.service.impl;

import com.qwli7.blog.BlogContext;
import com.qwli7.blog.entity.CommentModule;
import com.qwli7.blog.entity.Moment;
import com.qwli7.blog.entity.MomentArchive;
import com.qwli7.blog.entity.MomentNav;
import com.qwli7.blog.entity.dto.PageDto;
import com.qwli7.blog.entity.vo.MomentQueryParam;
import com.qwli7.blog.event.MomentDeleteEvent;
import com.qwli7.blog.event.MomentPostEvent;
import com.qwli7.blog.exception.LogicException;
import com.qwli7.blog.exception.ResourceNotFoundException;
import com.qwli7.blog.mapper.CommentMapper;
import com.qwli7.blog.mapper.MomentMapper;
import com.qwli7.blog.service.CommentModuleHandler;
import com.qwli7.blog.service.Markdown2Html;
import com.qwli7.blog.service.MomentService;
import com.qwli7.blog.util.JsoupUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qwli7
 * 2021/2/22 13:09
 * 功能：MomentServiceImpl
 **/
@Service
public class MomentServiceImpl implements MomentService, CommentModuleHandler {

    private final MomentMapper momentMapper;
    private final ApplicationEventPublisher publisher;
    private final Markdown2Html markdown2Html;
    private final CommentMapper commentMapper;

    public MomentServiceImpl(MomentMapper momentMapper, Markdown2Html markdown2Html,
                             CommentMapper commentMapper,
                             ApplicationEventPublisher publisher) {
        this.momentMapper = momentMapper;
        this.markdown2Html = markdown2Html;
        this.commentMapper = commentMapper;
        this.publisher = publisher;
    }

    /**
     * 保存动态
     * @param moment moment
     * @return int
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public int saveMoment(Moment moment) {
        moment.setHits(0);
        moment.setComments(0);
        moment.setCreateAt(LocalDateTime.now());
        moment.setModifyAt(LocalDateTime.now());
        momentMapper.insert(moment);
        publisher.publishEvent(new MomentPostEvent(this, moment));
        return moment.getId();
    }

    /**
     * 更新动态
     * @param moment moment
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void update(Moment moment) {
        momentMapper.findById(moment.getId()).orElseThrow(()
                -> new ResourceNotFoundException("moment.notExists", "动态不存在"));
        moment.setModifyAt(LocalDateTime.now());
        momentMapper.update(moment);
    }

    /**
     * 删除动态
     * @param id id
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void delete(int id) {
        final Moment oldMoment = momentMapper.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("moment.notExists", "动态不存在"));
        momentMapper.deleteById(id);
        commentMapper.deleteByModule(new CommentModule(id, getModuleName()));
        publisher.publishEvent(new MomentDeleteEvent(this, oldMoment));
    }

    /**
     * 更新点击量
     * @param id id
     * @param hits hits
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateHits(int id, int hits) {
        final Optional<Moment> momentOp = momentMapper.findById(id);
        if(!momentOp.isPresent()) {
            throw new ResourceNotFoundException("moment.notExists", "动态不存在");
        }
        final Moment moment = momentOp.get();
        momentMapper.addHits(moment.getId(), 1);
    }

    /**
     * 获取动态用以编辑
     * @param id id
     * @return Moment
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<Moment> findMomentForEdit(int id) {
        return momentMapper.findById(id);
    }

    /**
     * 获取动态
     * @param id id
     * @return Moment
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<Moment> findMoment(int id) {
        final Optional<Moment> momentOp = momentMapper.findById(id);
        if(!momentOp.isPresent()) {
            throw new ResourceNotFoundException("moment.notExists", "动态不存在");
        }
        final Moment moment = momentOp.get();
        if(moment.getPrivate() && !BlogContext.isAuthenticated()) {
            throw new LogicException("access.reject", "访问拒绝");
        }
        processMoment(moment);
        return Optional.of(moment);
    }

    /**
     * 分页查询动态
     * @param queryParam queryParam
     * @return PageDto<Moment>
     */
    @Transactional(readOnly = true)
    @Override
    public PageDto<Moment> findPage(MomentQueryParam queryParam) {
        int count = momentMapper.count(queryParam);
        if(count == 0) {
            return new PageDto<>(queryParam, 0, new ArrayList<>());
        }
        List<Moment> moments = momentMapper.findPage(queryParam);
        if(CollectionUtils.isEmpty(moments)) {
            return new PageDto<>(queryParam, 0, new ArrayList<>());
        }
        PageDto<Moment> pageDto = new PageDto<>(queryParam, count, moments);
        processMoments(pageDto.getData());
        return pageDto;
    }

    /**
     * 分页查询动态
     * 归档
     * @param queryParam queryParam
     * @return PageDto
     */
    @Transactional(readOnly = true)
    @Override
    public PageDto<MomentArchive> findArchivePage(MomentQueryParam queryParam) {
        int count = momentMapper.countArchive(queryParam);
        if(count == 0) {
            return new PageDto<>(queryParam, 0, new ArrayList<>());
        }

        final List<MomentArchive> momentArchives = momentMapper.findArchivePage(queryParam);
        if(CollectionUtils.isEmpty(momentArchives)) {
            return new PageDto<>(queryParam, 0, new ArrayList<>());
        }
        momentArchives.forEach(e -> {
            final List<Moment> moments = e.getMoments();
            processMoments(moments);
        });

        return new PageDto<>(queryParam, count, momentArchives);
    }

    /**
     * 获取动态上下导航
     * @param id id
     * @return MomentNav
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<MomentNav> findMomentNav(int id) {
        Moment current = momentMapper.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("moment.notExists", "动态不存在"));
        MomentNav momentNav = new MomentNav();
        final Optional<Moment> preMomentOp = momentMapper.findPreMoment(current);
        final Optional<Moment> nextMomentOp = momentMapper.findNextMoment(current);
        nextMomentOp.ifPresent(momentNav::setNextMoment);
        preMomentOp.ifPresent(momentNav::setPrevMoment);
        return Optional.of(momentNav);
    }

    /**
     * 处理动态
     * @param moment moment
     */
    private void processMoment(Moment moment) {
        processMoments(Collections.singletonList(moment));
    }

    /**
     * 批量处理动态
     * @param moments moments
     */
    private void processMoments(List<Moment> moments) {
        Map<Integer, String> contentMap = moments.stream().filter(m -> m.getContent() != null)
                .collect(Collectors.toMap(Moment::getId, Moment::getContent));
        if(contentMap.isEmpty()) {
            return;
        }
        Map<Integer, String> markdownMap = markdown2Html.toHtmls(contentMap);
        moments.forEach(e -> {
            JsoupUtil.getFirstImage(markdownMap.get(e.getId())).ifPresent(e::setFeatureImage);
            e.setContent(markdownMap.get(e.getId()));
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void hits(int id) {
        final Moment moment = momentMapper.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("moment.notExists", "动态不存在"));
        // 登录的情况下不增加点击量
        if(BlogContext.isAuthenticated()) {
            return;
        }
        momentMapper.updateHits(id, moment.getHits()+1);
    }

    @Transactional(readOnly = true)
    @Override
    public MomentArchive findLatestMoments() {
        MomentQueryParam queryParam = new MomentQueryParam();
        MomentArchive momentArchive =  momentMapper.findLatestMoments(queryParam);
        if(momentArchive == null) {
            return null;
        }
        final List<Moment> moments = momentArchive.getMoments();
        if(!CollectionUtils.isEmpty(moments)) {
            processMoments(moments);
        }
        return momentArchive;
    }

    /**
     * 获取模块名称
     * @return String
     */
    @Override
    public String getModuleName() {
        return Moment.class.getSimpleName().toLowerCase();
    }

    /**
     * 插入评论之前校验动态
     * @param module module
     */
    @Override
    public void validateBeforeInsert(CommentModule module) {
        Assert.notNull(module, "commentModule not null.");
        final Integer id = module.getId();
        final Optional<Moment> momentOp = momentMapper.findById(id);
        if(!momentOp.isPresent()) {
            throw new LogicException("comment.notExists", "动态不存在");
        }
        final Moment moment = momentOp.get();

        if(moment.getPrivate() && !BlogContext.isAuthenticated()) {
            throw new LogicException("comment.private", "不允许操作私人动态");
        }

        if(!moment.getAllowComment()) {
            throw new LogicException("moment.notAllow", "动态不允许评论");
        }
    }

    /**
     * 查询评论之前校验动态
     * @param module module
     */
    @Override
    public void validateBeforeQuery(CommentModule module) {
        final Integer id = module.getId();
        final String name = module.getName();
        if(!getModuleName().equals(name)) {
            throw new LogicException("invalid.module", "无效的模块");
        }
        final Moment moment = momentMapper.findById(id).orElseThrow(()
                -> new LogicException("moment.notExists", "动态不存在"));

        if(moment.getPrivate() && !BlogContext.isAuthenticated()) {
            throw new LogicException("illegal.operator", "无效的操作");
        }
    }
}

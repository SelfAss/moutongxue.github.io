package com.qwli7.blog.template.data;

import com.qwli7.blog.entity.Moment;
import com.qwli7.blog.service.MomentService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LatestMomentsDataProvider extends AbstractDataProvider<List<Moment>> {

    private final MomentService momentService;

    public LatestMomentsDataProvider(MomentService momentService) {
        super("latestMoment");
        this.momentService = momentService;
    }

    @Override
    public List<Moment> queryData(Map<String, String> attributeMap) {
        return null;
    }
}
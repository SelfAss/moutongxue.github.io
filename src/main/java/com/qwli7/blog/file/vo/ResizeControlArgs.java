package com.qwli7.blog.file.vo;

import com.qwli7.blog.file.Resize;

/**
 * 缩放控制参数
 * @author liqiwen
 * @since 2.4
 */
public class ResizeControlArgs extends ControlArgs {

    private Resize resize;

    public Resize getResize() {
        return resize;
    }

    public void setResize(Resize resize) {
        this.resize = resize;
    }
}

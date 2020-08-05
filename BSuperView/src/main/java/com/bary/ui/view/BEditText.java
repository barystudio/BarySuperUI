package com.bary.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatEditText;

import com.bary.ui.R;
import com.bary.ui.view.builder.GradientBuilder;
import com.bary.ui.view.eum.GradientOrientation;
import com.bary.ui.view.eum.GradientType;
import com.bary.ui.view.interf.IGradientInterface;
import com.bary.ui.view.interf.ISuperInterface;
import com.bary.ui.view.interf.IRoundInterface;
import com.bary.ui.view.interf.IShadowInterface;
import com.bary.ui.view.interf.IBorderInterface;
import com.bary.ui.view.builder.RoundBuilder;
import com.bary.ui.view.builder.ShadowBuilder;
import com.bary.ui.view.builder.BorderBuilder;

import java.util.List;


/**
 * 自定义TextView
 * author Bary
 * create at 2020/1/21 11:27
 */
public class BEditText extends AppCompatEditText implements IShadowInterface, IBorderInterface, IRoundInterface, IGradientInterface, ISuperInterface {
    private RoundBuilder mRoundBuilder;
    private ShadowBuilder mShadowBuilder;
    private BorderBuilder mBorderBuilder;
    private GradientBuilder mGradientBuilder;

    /**
     * 清楚按钮的图标/密码可见/不可见的图标
     */
    private Drawable drawableClear, drawablePasswordVisible, drawablePasswordInvisible;
    /**
     * 文本变化前长度
     */
    private int mBeforeTextLength = 0;
    /**
     * 变化中文本长度
     */
    private int mChangingTextLength = 0;
    /**
     * 文本是否有变化
     */
    private boolean isChanged = false;
    /**
     * 光标位置
     */
    private int mSelectionLoc = 0;

    /**
     * 是否是银行卡类型
     **/
    private boolean isBankNoType;
    /**
     * 银行卡类型空格数量
     */
    private int mBankSpaceNumberB = 0;

    /**
     * 最大长度
     **/
    private int maxLength;

    public int mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom;

    /**
     * 右侧图标类型
     **/
    private Enum mDrawableType = DrawableEnum.CLEAR;



    private enum DrawableEnum {
        VISIBE_PWD,//显示密码图标
        INVISIBE_PWD,//隐藏密码图标
        CLEAR//清空文本图标
    }


    private StringBuffer mTextBuffer = new StringBuffer();

    public BEditText(Context context) {
        this(context, null);
    }

    public BEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        mPaddingTop = getPaddingTop();
        mPaddingBottom = getPaddingBottom();
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.BEditText);
        try {
            // 初始化阴影样式
            mShadowBuilder = new ShadowBuilder(this, this);
            // 初始化基础样式
            mBorderBuilder = new BorderBuilder(this, this);
            // 初始化描边样式
            mRoundBuilder = new RoundBuilder(this, this);
            // 初始化渐变样式
            mGradientBuilder = new GradientBuilder(this, this);

            mShadowBuilder.initAttributes(attr);
            mBorderBuilder.initAttributes(attr);
            mRoundBuilder.initAttributes(attr);
            mGradientBuilder.initAttributes(attr);

        } finally {
            attr.recycle();
        }
        setFocusableInTouchMode(true);
        // 获取自定义属性
        drawableClear = getResources().getDrawable(R.drawable.fty_edit_icon_del);
        drawablePasswordVisible = getResources().getDrawable(R.drawable.fty_edit_icon_visible);
        drawablePasswordInvisible = getResources().getDrawable(R.drawable.fty_edit_icon_invisible);
        setTag(R.id.fty_id_edit_show_password, false);
        updateIcon();
        // 设置TextWatcher用于更新清除按钮显示状态
        addTextChangedListener(mTextWatcher);
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mOnFocusChangeListener != null) {
                    mOnFocusChangeListener.onFocusChange(v, hasFocus);
                }
                updateIcon();
            }
        });
    }

    private OnFocusChangeListener mOnFocusChangeListener;

    public void setOnNewFocusChangeListener(OnFocusChangeListener listener) {
        mOnFocusChangeListener = listener;
    }

    public void setHintSize(int size) {
        SpannableString ss = new SpannableString(getHint());//定义hint的值
        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(size, false);//设置字体大小 true表示单位是sp
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        setHint(new SpannedString(ss));
    }

    /**
     * 本文最大长度
     *
     * @param maxLength 长度
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        invalidate();
    }

    /**
     * 显示密码可见图标
     */
    public void showPwdVisibeIcon() {
        mDrawableType = DrawableEnum.INVISIBE_PWD;
        post(new Runnable() {
            @Override
            public void run() {
                updateIcon();
            }
        });
    }

    /**
     * 设置密码可见图标
     */
    public void setPwdVisibeIcon(@DrawableRes int visibeIcon, @DrawableRes int invisibeIcon) {
        drawablePasswordVisible = getResources().getDrawable(visibeIcon);
        drawablePasswordInvisible = getResources().getDrawable(invisibeIcon);
        post(new Runnable() {
            @Override
            public void run() {
                updateIcon();
            }
        });
    }

    /**
     * 银行卡类型
     *
     * @param bankNoType 是否是银行卡类型
     */
    public void setBankNoType(boolean bankNoType) {
        isBankNoType = bankNoType;
        invalidate();
    }

    /**
     * 字符变化监听
     */
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (isBankNoType) {
                mBeforeTextLength = s.length();
                if (mTextBuffer.length() > 0) {
                    mTextBuffer.delete(0, mTextBuffer.length());
                }
                mBankSpaceNumberB = 0;
                for (int i = 0; i < s.length(); i++) {
                    if (s.charAt(i) == ' ') {
                        mBankSpaceNumberB++;
                    }
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (isBankNoType) {

                mChangingTextLength = s.length();
                mTextBuffer.append(s.toString());
                if (mChangingTextLength == mBeforeTextLength || mChangingTextLength <= 3 || isChanged) {
                    isChanged = false;
                    return;
                }
                isChanged = true;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateIcon();
            if (isChanged && isBankNoType) {
                mSelectionLoc = getSelectionEnd();
                int index = 0;
                while (index < mTextBuffer.length()) {
                    if (mTextBuffer.charAt(index) == ' ') {
                        mTextBuffer.deleteCharAt(index);
                    } else {
                        index++;
                    }
                }

                index = 0;
                int konggeNumberC = 0;
                while (index < mTextBuffer.length()) {
                    if ((index == 4 || index == 9 || index == 14 || index == 19)) {
                        mTextBuffer.insert(index, ' ');
                        konggeNumberC++;
                    }
                    index++;
                }

                if (konggeNumberC > mBankSpaceNumberB) {
                    mSelectionLoc += (konggeNumberC - mBankSpaceNumberB);
                }

                char[] tempChar = new char[mTextBuffer.length()];
                mTextBuffer.getChars(0, mTextBuffer.length(), tempChar, 0);
                String str = mTextBuffer.toString();
                if (mSelectionLoc > str.length()) {
                    mSelectionLoc = str.length();
                } else if (mSelectionLoc < 0) {
                    mSelectionLoc = 0;
                }

                setText(str);
                Editable etable = getText();
                Selection.setSelection(etable, mSelectionLoc);
                isChanged = false;
            }
        }
    };


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int xDown = (int) event.getX();
        if (event.getAction() == MotionEvent.ACTION_DOWN && xDown >= (getWidth() - getCompoundPaddingRight() * 2) && xDown < getWidth() && isEnabled()) {
            // 清除按钮的点击范围 按钮自身大小 +-padding
            if (mDrawableType == DrawableEnum.CLEAR) {
                clearText();
            } else if (mDrawableType == DrawableEnum.INVISIBE_PWD) {
                // 显示密码
                setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                setTag(R.id.fty_id_edit_show_password, true);
                // 使光标始终在最后位置
                Editable etable = getText();
                Selection.setSelection(etable, etable.length());
                mDrawableType = DrawableEnum.VISIBE_PWD;
                updateIcon();
            } else if (mDrawableType == DrawableEnum.VISIBE_PWD) {
                // 隐藏密码
                setTransformationMethod(PasswordTransformationMethod.getInstance());
                setTag(R.id.fty_id_edit_show_password, false);
                // 使光标始终在最后位置
                Editable etable = getText();
                Selection.setSelection(etable, etable.length());
                mDrawableType = DrawableEnum.INVISIBE_PWD;
                updateIcon();
            }
            return false;
        }
        super.onTouchEvent(event);
        return true;
    }


    /**
     * 更新清除按钮图标显示
     */
    private void updateIcon() {
        Drawable mDrawable = null;
        // 获取设置好的drawableLeft、drawableTop、drawableRight、drawableBottom
        Drawable[] drawables = getCompoundDrawables();
        if (mDrawableType == DrawableEnum.INVISIBE_PWD) {
            mDrawable = drawablePasswordInvisible;
        } else if (mDrawableType == DrawableEnum.VISIBE_PWD) {
            mDrawable = drawablePasswordVisible;
        } else if (mDrawableType == DrawableEnum.CLEAR) {
            if (length() > 0 && isFocused()) {
                mDrawable = drawableClear;
            }
        }
        if (mDrawable != null) {
            int size = getHeight() / 2;
//            if(size>PxTransUtils.auto2px(60)){
//                size = (int) PxTransUtils.auto2px(60);
//            }
            mDrawable.setBounds(0, 0, size, size);
        }
        setCompoundDrawables(drawables[0], drawables[1], mDrawable,
                drawables[3]);
    }


    /**
     * 清空文本的方法
     */
    public void clearText() {
        setText("");
    }


    public void setLeftIcon(Drawable drawable, int width, int height, int padding) {
        setDrawable("left", drawable, width, height, padding);
    }

    public void setTopIcon(Drawable drawable, int width, int height, int padding) {
        setDrawable("top", drawable, width, height, padding);
    }

    public void setRightIcon(Drawable drawable, int width, int height, int padding) {
        setDrawable("right", drawable, width, height, padding);
    }

    public void setBottomIcon(Drawable drawable, int width, int height, int padding) {
        setDrawable("bottom", drawable, width, height, padding);
    }

    private void setDrawable(String type, Drawable drawable, int width, int height, int padding) {
        drawable.setBounds(0, 0, width <= 0 ? drawable.getMinimumWidth() : width, height <= 0 ? drawable.getMinimumHeight() : height);
        Drawable[] drawables = getCompoundDrawables();
        switch (type) {
            case "left":
                drawables[0] = drawable;
                break;
            case "top":
                drawables[1] = drawable;
                break;
            case "right":
                drawables[2] = drawable;
                break;
            case "bottom":
                drawables[3] = drawable;
                break;
        }
        setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
        setCompoundDrawablePadding(padding);//设置图片和text之间的间距
    }

    @Override
    public float getRoundRadius() {
        return mRoundBuilder.getRoundRadius();
    }

    @Override
    public void setRoundRadius(float radius) {
        mRoundBuilder.setRoundRadius(radius);
    }

    @Override
    public float getTopLeftRoundRadius() {
        return mRoundBuilder.getTopLeftRoundRadius();
    }

    @Override
    public void setTopLeftRoundRadius(float radius) {
        mRoundBuilder.setTopLeftRoundRadius(radius);
    }

    @Override
    public float getTopRightRoundRadius() {
        return mRoundBuilder.getTopRightRoundRadius();
    }

    @Override
    public void setTopRightRoundRadius(float radius) {
        mRoundBuilder.setTopRightRoundRadius(radius);
    }

    @Override
    public float getBottomLeftRoundRadius() {
        return mRoundBuilder.getBottomLeftRoundRadius();
    }

    @Override
    public void setBottomLeftRoundRadius(float radius) {
        mRoundBuilder.setBottomLeftRoundRadius(radius);
    }

    @Override
    public float getBottomRightRoundRadius() {
        return mRoundBuilder.getBottomRightRoundRadius();
    }

    @Override
    public void setBottomRightRoundRadius(float radius) {
        mRoundBuilder.setBottomRightRoundRadius(radius);
    }

    @Override
    public void setBackgroundColor(int color) {
        mShadowBuilder.setBackgroundColor(color);
    }

    @Override
    public int getBackgroundColor() {
        return mShadowBuilder.getBackgroundColor();
    }

    @Override
    public void setShadowDx(float dx) {
        mShadowBuilder.setShadowDx(dx);
    }

    @Override
    public float getShadowDx() {
        return mShadowBuilder.getShadowDx();
    }

    @Override
    public void setShadowDy(float dy) {
        mShadowBuilder.setShadowDy(dy);
    }

    @Override
    public float getShadowDy() {
        return mShadowBuilder.getShadowDy();
    }

    @Override
    public float getShadowXSize() {
        return mShadowBuilder.getShadowXSize();
    }

    @Override
    public void setShadowYSize(float size) {
        mShadowBuilder.setShadowYSize(size);
    }

    @Override
    public float getShadowYSize() {
        return mShadowBuilder.getShadowYSize();
    }

    @Override
    public float getShadowSize() {
        return mShadowBuilder.getShadowSize();
    }

    @Override
    public void setShadowSize(float size) {
        mShadowBuilder.setShadowSize(size);
    }

    @Override
    public void setShadowXSize(float size) {
        mShadowBuilder.setShadowXSize(size);
    }

    @Override
    public void setShadowColor(int color) {
        mShadowBuilder.setShadowColor(color);
    }

    @Override
    public int getShadowColor() {
        return mShadowBuilder.getShadowColor();
    }

    @Override
    public void setShadowAlpha(float alpha) {
        mShadowBuilder.setShadowAlpha(alpha);
    }

    @Override
    public float getShadowAlpha() {
        return mShadowBuilder.getShadowAlpha();
    }

    @Override
    public void showShadowEdges(int... edges) {
        mShadowBuilder.showShadowEdges(edges);
    }

    @Override
    public void hideShadowEdges(int... edges) {
        mShadowBuilder.hideShadowEdges(edges);
    }

    @Override
    public boolean isHiddenShadowEdges(int edges) {
        return mShadowBuilder.isHiddenShadowEdges(edges);
    }


    @Override
    public boolean isShadowShow() {
        return mShadowBuilder.isShadowShow();
    }

    @Override
    public float getBorderSize() {
        return mBorderBuilder.getBorderSize();
    }

    @Override
    public void setBorderSize(float size) {
        mBorderBuilder.setBorderSize(size);
    }

    @Override
    public void setBorderColor(int color) {
        mBorderBuilder.setBorderColor(color);
    }

    @Override
    public int getBorderColor() {
        return mBorderBuilder.getBorderColor();
    }

    @Override
    public boolean isBorderShow() {
        return mBorderBuilder.isBorderShow();
    }

    @Override
    public void hideBorderEdges(int... edges) {
        mBorderBuilder.hideBorderEdges(edges);
    }

    @Override
    public void showBorderEdges(int... edges) {
        mBorderBuilder.showBorderEdges(edges);
    }

    @Override
    public boolean isHiddenBorderEdges(int edges) {
        return mBorderBuilder.isHiddenBorderEdges(edges);
    }

    @Override
    public List<String> getTextGradientColor() {
        return mGradientBuilder.getTextGradientColor();
    }

    @Override
    public void setTextGradientColor(String... colors) {
        mGradientBuilder.setTextGradientColor(colors);
    }

    @Override
    public void clearTextGradientColor() {
        mGradientBuilder.clearTextGradientColor();
    }

    @Override
    public GradientOrientation getTextGradientOrientation() {
        return mGradientBuilder.getTextGradientOrientation();
    }

    @Override
    public void setTextGradientOrientation(GradientOrientation orientation) {
        mGradientBuilder.setTextGradientOrientation(orientation);
    }

    @Override
    public GradientType getTextGradientType() {
        return mGradientBuilder.getTextGradientType();
    }

    @Override
    public void setTextGradientType(GradientType type) {
        mGradientBuilder.setTextGradientType(type);
    }

    @Override
    public List<String> getBackgroundGradientColor() {
        return mGradientBuilder.getBackgroundGradientColor();
    }

    @Override
    public void setBackgroundGradientColor(String... colors) {
        mGradientBuilder.setBackgroundGradientColor(colors);
    }

    @Override
    public void clearBackgroundGradientColor() {
        mGradientBuilder.clearBackgroundGradientColor();
    }

    @Override
    public GradientOrientation getBackgroundGradientOrientation() {
        return mGradientBuilder.getBackgroundGradientOrientation();
    }

    @Override
    public void setBackgroundGradientOrientation(GradientOrientation orientation) {
        mGradientBuilder.setBackgroundGradientOrientation(orientation);
    }

    @Override
    public GradientType getBackgroundGradientType() {
        return mGradientBuilder.getBackgroundGradientType();
    }

    @Override
    public void setBackgroundGradientType(GradientType type) {
        mGradientBuilder.setBackgroundGradientType(type);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mShadowBuilder.updateUI();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingRight = right;
        mPaddingTop = top;
        mPaddingBottom = bottom;
        mShadowBuilder.updateUI();
    }

    @Override
    public RoundBuilder getBasicStyle() {
        return mRoundBuilder;
    }

    @Override
    public ShadowBuilder getShadowStyle() {
        return mShadowBuilder;
    }

    @Override
    public BorderBuilder getBorderStyle() {
        return mBorderBuilder;
    }

    @Override
    public GradientBuilder getGradientStyle() {
        return mGradientBuilder;
    }

    @Override
    public void updatePadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
    }

    @Override
    public Padding getDefPadding() {
        return new Padding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        mGradientBuilder.updateTextGradient();
        super.onDraw(canvas);
    }


}


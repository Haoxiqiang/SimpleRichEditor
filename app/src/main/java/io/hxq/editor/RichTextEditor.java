package io.hxq.editor;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.StringDef;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是一个富文本编辑器，给外部提供insertImage接口，添加的图片跟当前光标所在位置有关
 */
public class RichTextEditor extends LinearLayout implements View.OnFocusChangeListener {

    // edittext常规padding是10dp
    private static final int EDIT_PADDING = 10;

    // 新生的view都会打一个tag，对每个view来说，这个tag是唯一的。
    private int viewTagIndex = 1;

    // 所有EditText的软键盘监听器
    // 主要用来处理点击回删按钮时，view的一些列合并操作
    private OnKeyListener onEditKeyListener = new OnKeyListener() {

        @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (v instanceof EditText
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                onBackspacePress((EditText) v);
            }
            return false;
        }
    };
    // 图片右上角红叉按钮监听器
    private OnClickListener imageCloseListener = new OnClickListener() {
        @Override public void onClick(View v) {
            Log.i("imageClose", "imageCloseListener");
            onImageCloseClick(v);
        }
    };

    private EditText lastFocusEdit; // 最近被聚焦的EditText
    private LayoutTransition layoutTransition; // 只在图片View添加或remove时，触发transition动画
    private int editNormalPadding = 0; //
    private int disappearingImageIndex = 0;

    public RichTextEditor(Context context) {
        this(context, null);
    }

    public RichTextEditor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RichTextEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(LinearLayout.VERTICAL);

        removeAllViews();

        setupLayoutTransitions();

        LinearLayout.LayoutParams firstEditParam =
            new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        editNormalPadding =
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EDIT_PADDING,
                getResources().getDisplayMetrics());
        final EditText firstEdit = createEditText("input here");
        lastFocusEdit = firstEdit;
        addView(firstEdit, firstEditParam);

        setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                int count = getChildCount();
                for (int i = count - 1; i >= 0; i--) {
                    View last = getChildAt(i);
                    Log.e("Editor", "i:" + i + "  " + last.getClass().getSimpleName());
                    if (last instanceof DelegateEditText) {
                        showKeyboard((EditText) last);
                        break;
                    }
                }
            }
        });
    }

    /**
     * 处理软键盘backSpace回退事件
     *
     * @param editTxt 光标所在的文本输入框
     */
    private void onBackspacePress(EditText editTxt) {
        int startSelection = editTxt.getSelectionStart();
        // 只有在光标已经顶到文本输入框的最前方，在判定是否删除之前的图片，或两个View合并
        if (startSelection == 0) {
            int editIndex = indexOfChild(editTxt);
            View preView = getChildAt(editIndex - 1); // 如果editIndex-1<0,
            // 则返回的是null
            if (null != preView) {
                if (preView instanceof RelativeLayout) {
                    // 光标EditText的上一个view对应的是图片
                    onImageCloseClick(preView);
                } else if (preView instanceof EditText) {
                    // 光标EditText的上一个view对应的还是文本框EditText
                    String str1 = editTxt.getText().toString();
                    EditText preEdit = (EditText) preView;
                    String str2 = preEdit.getText().toString();

                    // 合并文本view时，不需要transition动画
                    setLayoutTransition(null);
                    removeView(editTxt);
                    setLayoutTransition(layoutTransition); // 恢复transition动画

                    // 文本合并
                    preEdit.setText(String.format("%s%s", str2, str1));
                    preEdit.requestFocus();
                    preEdit.setSelection(str2.length(), str2.length());
                    lastFocusEdit = preEdit;
                }
            }
        }
    }

    /**
     * 处理图片叉掉的点击事件
     *
     * @param view 整个image对应的relativeLayout view
     * @type 删除类型 0代表backspace删除 1代表按红叉按钮删除
     */
    private void onImageCloseClick(View view) {
        Log.i("imageClose", "onImageCloseClick");
        if (!layoutTransition.isRunning()) {
            Object tag = view.getTag();
            Log.i("imageClose", "tag:" + tag);
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getTag() == tag) {
                    disappearingImageIndex = indexOfChild(child);
                    removeView(child);
                    break;
                }
            }
        }
    }

    /**
     * 生成文本输入框
     */
    private EditText createEditText(String hint) {

        EditText editText = (EditText) View.inflate(getContext(), R.layout.editor_text, null);
        editText.setOnKeyListener(onEditKeyListener);
        editText.setTag(viewTagIndex++);
        editText.setPadding(editNormalPadding, editNormalPadding, editNormalPadding, 0);
        editText.setHint(hint);
        editText.setGravity(Gravity.CLIP_VERTICAL | Gravity.LEFT);
        //EditText的焦点监听listener
        editText.setOnFocusChangeListener(this);
        return editText;
    }

    /**
     * 生成图片View
     */
    private DelegateImage createImageLayout() {
        Object tag = viewTagIndex++;
        DelegateImage delegateImage = new DelegateImage(getContext());
        delegateImage.setTag(tag);
        delegateImage.setOnImageCloseListener(tag, imageCloseListener);
        return delegateImage;
    }

    /**
     * 根据绝对路径添加view
     */
    public void insertTale(Tale tale) {

        String content = lastFocusEdit.getText().toString();
        int cursor = lastFocusEdit.getSelectionStart();

        String pre = content.substring(0, cursor).trim();
        String next = content.substring(cursor).trim();

        int index = indexOfChild(lastFocusEdit);
        Log.e("insertTale", "content:"
            + content
            + "  cursor:"
            + cursor
            + "  pre:"
            + pre
            + "  next:"
            + next
            + "  index:"
            + index);

        if (content.length() == 0 || pre.length() == 0) {
            // 如果EditText为空，或者光标已经顶在了editText的最前面，则直接插入图片，并且EditText下移即可
            addImageViewAtIndex(index, tale);
        } else {
            // 如果EditText非空且光标不在最顶端，则需要添加新的imageView和EditText

            if (getChildCount() - 1 == index || next.length() > 0) {
                lastFocusEdit.setText(pre);
                addEditTextAtIndex(index + 1, next);
            }

            addImageViewAtIndex(index + 1, tale);
            lastFocusEdit.setText(next);
            lastFocusEdit.requestFocus();
            lastFocusEdit.setSelection(next.length());
            Log.e("insertTale", "  selection:" + pre.length() + "  editStr2:" + next);
        }
        hideKeyBoard();
    }

    /**
     * 在特定位置添加ImageView
     */
    private void addImageViewAtIndex(final int index, Tale tale) {
        final DelegateImage imageLayout = createImageLayout();
        imageLayout.setTale(tale);
        addView(imageLayout, index);
    }

    /**
     * 隐藏小键盘
     */
    public void hideKeyBoard() {
        InputMethodManager imm =
            (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(lastFocusEdit.getWindowToken(), 0);
        }
    }

    private void showKeyboard(EditText v) {
        InputMethodManager inputManager =
            (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            v.requestFocus();
            v.setSelection(v.getText().toString().length());
            inputManager.showSoftInput(v, InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }

    /**
     * 在特定位置插入EditText
     *
     * @param index 位置
     * @param editStr EditText显示的文字
     */
    private void addEditTextAtIndex(final int index, String editStr) {
        EditText editText2 = createEditText("");
        editText2.setText(editStr);
        lastFocusEdit = editText2;
        // 请注意此处，EditText添加、或删除不触动Transition动画
        setLayoutTransition(null);
        addView(editText2, index);
        setLayoutTransition(layoutTransition); // remove之后恢复transition动画
    }

    /**
     * 初始化transition动画
     */
    private void setupLayoutTransitions() {
        layoutTransition = new LayoutTransition();
        setLayoutTransition(layoutTransition);
        layoutTransition.addTransitionListener(new TransitionListener() {

            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container, View view,
                int transitionType) {

            }

            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container, View view,
                int transitionType) {
                if (!transition.isRunning()
                    && transitionType == LayoutTransition.CHANGE_DISAPPEARING) {
                    // transition动画结束，合并EditText
                    //mergeEditText();
                }
            }
        });
        layoutTransition.setDuration(120);
    }

    /**
     * 图片删除的时候，如果上下方都是EditText，则合并处理
     */
    private void mergeEditText() {
        View preView = getChildAt(disappearingImageIndex - 1);
        View nextView = getChildAt(disappearingImageIndex);
        if (preView != null
            && preView instanceof EditText
            && null != nextView
            && nextView instanceof EditText) {
            Log.d("LeiTest", "合并EditText");
            EditText preEdit = (EditText) preView;
            EditText nextEdit = (EditText) nextView;
            String str1 = preEdit.getText().toString();
            String str2 = nextEdit.getText().toString();
            String mergeText = "";
            if (str2.length() > 0) {
                mergeText = str1 + "\n" + str2;
            } else {
                mergeText = str1;
            }

            setLayoutTransition(null);
            removeView(nextEdit);
            preEdit.setText(mergeText);
            preEdit.requestFocus();
            preEdit.setSelection(str1.length(), str1.length());
            setLayoutTransition(layoutTransition);
        }
    }

    /**
     * 对外提供的接口, 生成编辑数据上传
     */
    public ArrayList<Tale> getTale() {
        ArrayList<Tale> dataList = new ArrayList<>();
        int paragraphCount = getChildCount();
        for (int index = 0; index < paragraphCount; index++) {
            View child = getChildAt(index);
            if (child instanceof DelegateEditText) {
                EditText text = (EditText) child;
                Tale item = new Tale();
                item.txt = text.getText().toString();
                item.type = Tale.Type.Text;
                dataList.add(item);
            } else if (child instanceof DelegateImage) {
                dataList.add(((DelegateImage) child).getTale());
            }
        }

        return dataList;
    }

    @Override public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus && v instanceof EditText) {
            lastFocusEdit = (EditText) v;
        }
    }

    @MainThread public void setTale(List<Tale> tales, boolean editable) {

        if (tales == null || tales.isEmpty()) {
            return;
        }
        removeAllViews();
        for (int i = 0; i < tales.size(); i++) {
            Tale tale = tales.get(i);
            if (RichTextEditor.Tale.Type.Text.equals(tale.type)) {
                addEditTextAtIndex(i, tale.txt);
            } else {
                addImageViewAtIndex(i, tale);
            }
        }
        setEditable(editable);
    }

    public static class Tale implements Parcelable{

        @Retention(RetentionPolicy.SOURCE) @StringDef({ Type.Text, Type.Image }) @interface Type {
            String Text = "txt";
            String Image = "img";
        }

        @Type String type;
        String txt;
        long id;
        int w;
        int h;
        String fmt;
        String path;

        @Override public int describeContents() {
            return 0;
        }

        @Override public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.type);
            dest.writeString(this.txt);
            dest.writeLong(this.id);
            dest.writeInt(this.w);
            dest.writeInt(this.h);
            dest.writeString(this.fmt);
            dest.writeString(this.path);
        }

        public Tale() {
        }

        protected Tale(Parcel in) {
            this.type = in.readString();
            this.txt = in.readString();
            this.id = in.readLong();
            this.w = in.readInt();
            this.h = in.readInt();
            this.fmt = in.readString();
            this.path = in.readString();
        }

        public static final Creator<Tale> CREATOR = new Creator<Tale>() {
            @Override public Tale createFromParcel(Parcel source) {
                return new Tale(source);
            }

            @Override public Tale[] newArray(int size) {
                return new Tale[size];
            }
        };
    }

    public void setEditable(boolean editable) {
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View last = getChildAt(i);
            if (last instanceof DelegateEditText) {
                DelegateEditText v = ((DelegateEditText) last);
                v.setInputType(editable ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
            }
        }
    }
}

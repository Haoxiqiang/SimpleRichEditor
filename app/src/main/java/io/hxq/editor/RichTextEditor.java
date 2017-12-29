package io.hxq.editor;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是一个富文本编辑器，给外部提供insertImage接口，添加的图片跟当前光标所在位置有关
 */
public class RichTextEditor extends LinearLayout implements View.OnFocusChangeListener {

  // edittext常规padding是10dp
  private static final int EDIT_PADDING = 10;
  // 第一个EditText的paddingTop值
  private static final int EDIT_FIRST_PADDING_TOP = 10;

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
      RelativeLayout parentView = (RelativeLayout) v.getParent();
      onImageCloseClick(parentView);
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
    editNormalPadding = dip2px(EDIT_PADDING);
    final EditText firstEdit = createEditText("input here");
    lastFocusEdit = firstEdit;
    addView(firstEdit, firstEditParam);

    setOnClickListener(new OnClickListener() {
      @Override public void onClick(View view) {
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
          View last = getChildAt(i);
          if (last instanceof EditText) {
            last.requestFocus();
            ((EditText) last).setSelection(((EditText) last).getText().toString().length());
            InputMethodManager inputManager =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
              inputManager.showSoftInput(last, InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
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
    if (!layoutTransition.isRunning()) {
      disappearingImageIndex = indexOfChild(view);
      removeView(view);
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
  private RelativeLayout createImageLayout() {
    RelativeLayout layout =
        (RelativeLayout) View.inflate(getContext(), R.layout.editor_image, null);
    layout.setTag(viewTagIndex++);
    View closeView = layout.findViewById(R.id.image_close);
    closeView.setTag(layout.getTag());
    closeView.setOnClickListener(imageCloseListener);
    return layout;
  }

  /**
   * 根据绝对路径添加view
   */
  public void insertImage(String imagePath) {

    String lastEditStr = lastFocusEdit.getText().toString();
    int cursorIndex = lastFocusEdit.getSelectionStart();
    String editStr1 = lastEditStr.substring(0, cursorIndex).trim();
    int lastEditIndex = indexOfChild(lastFocusEdit);

    if (lastEditStr.length() == 0 || editStr1.length() == 0) {
      // 如果EditText为空，或者光标已经顶在了editText的最前面，则直接插入图片，并且EditText下移即可
      addImageViewAtIndex(lastEditIndex, imagePath);
    } else {
      // 如果EditText非空且光标不在最顶端，则需要添加新的imageView和EditText
      lastFocusEdit.setText(editStr1);
      String editStr2 = lastEditStr.substring(cursorIndex).trim();
      if (getChildCount() - 1 == lastEditIndex || editStr2.length() > 0) {
        addEditTextAtIndex(lastEditIndex + 1, editStr2);
      }

      addImageViewAtIndex(lastEditIndex + 1, imagePath);
      lastFocusEdit.requestFocus();
      lastFocusEdit.setSelection(editStr1.length(), editStr1.length());
    }
    hideKeyBoard();

    //Bitmap bmp = getScaledBitmap(imagePath, getWidth());
    //insertImage(bmp, imagePath);
  }

  /**
   * 在特定位置添加ImageView
   */
  private void addImageViewAtIndex(final int index, String imagePath) {
    final RelativeLayout imageLayout = createImageLayout();

    ImageView imageView = imageLayout.findViewById(R.id.edit_imageView);

    Glide.with(getContext())
        .load(imagePath)
        .apply(RequestOptions.centerCropTransform().priority(Priority.HIGH))
        .into(imageView);

    // onActivityResult无法触发动画，此处post处理
    postDelayed(new Runnable() {
      @Override public void run() {
        addView(imageLayout, index);
      }
    }, 200);
  }

  /**
   * 隐藏小键盘
   */
  public void hideKeyBoard() {
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(lastFocusEdit.getWindowToken(), 0);
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

    // 请注意此处，EditText添加、或删除不触动Transition动画
    setLayoutTransition(null);
    addView(editText2, index);
    setLayoutTransition(layoutTransition); // remove之后恢复transition动画
  }

  /**
   * 根据view的宽度，动态缩放bitmap尺寸
   *
   * @param width view的宽度
   */
  private Bitmap getScaledBitmap(String filePath, int width) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(filePath, options);
    int sampleSize = options.outWidth > width ? options.outWidth / width + 1 : 1;
    options.inJustDecodeBounds = false;
    options.inSampleSize = sampleSize;
    return BitmapFactory.decodeFile(filePath, options);
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
        if (!transition.isRunning() && transitionType == LayoutTransition.CHANGE_DISAPPEARING) {
          // transition动画结束，合并EditText
          //mergeEditText();
        }
      }
    });
    layoutTransition.setDuration(300);
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
   * dp和pixel转换
   *
   * @param dipValue dp值
   * @return 像素值
   */
  public int dip2px(float dipValue) {
    float m = getContext().getResources().getDisplayMetrics().density;
    return (int) (dipValue * m + 0.5f);
  }

  /**
   * 对外提供的接口, 生成编辑数据上传
   */
  public List<EditData> buildEditData() {
    List<EditData> dataList = new ArrayList<EditData>();
    int num = getChildCount();
    for (int index = 0; index < num; index++) {
      View itemView = getChildAt(index);
      EditData itemData = new EditData();
      if (itemView instanceof EditText) {
        EditText item = (EditText) itemView;
        itemData.inputStr = item.getText().toString();
      } else if (itemView instanceof RelativeLayout) {

      }
      dataList.add(itemData);
    }

    return dataList;
  }

  @Override public void onFocusChange(View v, boolean hasFocus) {
    if (hasFocus && v instanceof EditText) {
      lastFocusEdit = (EditText) v;
    }
  }

  class EditData {
    String inputStr;
    String imagePath;
    Bitmap bitmap;
  }
}

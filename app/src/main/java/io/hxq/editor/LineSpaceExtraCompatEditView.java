package io.hxq.editor;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

public class LineSpaceExtraCompatEditView extends AppCompatEditText implements IGetLineSpaceExtra {

  private static final String TAG = LineSpaceExtraCompatEditView.class.getSimpleName();
  private Rect mRect;

  public LineSpaceExtraCompatEditView(Context context) {
    this(context, null);
  }

  public LineSpaceExtraCompatEditView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LineSpaceExtraCompatEditView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mRect = new Rect();
  }

  @Override public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    return new DeleteInputConnection(super.onCreateInputConnection(outAttrs), true);
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_ENTER) {
      // Just ignore the [Enter] key
      return true;
    }
    // Handle all other keys in the default way
    return super.onKeyDown(keyCode, event);
  }

  private class DeleteInputConnection extends InputConnectionWrapper {

    public DeleteInputConnection(InputConnection target, boolean mutable) {
      super(target, mutable);
    }

    @Override public boolean sendKeyEvent(KeyEvent event) {
      return super.sendKeyEvent(event);
    }

    @Override public boolean deleteSurroundingText(int beforeLength, int afterLength) {
      if (beforeLength == 1 && afterLength == 0) {
        return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
      }

      return super.deleteSurroundingText(beforeLength, afterLength);
    }
  }

  @Override public int getSpaceExtra() {
    return calculateExtraSpace();
  }

  /**
   * @return 算出最后一行多出的行间距的高
   */
  public int calculateExtraSpace() {
    int result = 0;
    int lastLineIndex = getLineCount() - 1;

    if (lastLineIndex >= 0) {
      Layout layout = getLayout();
      int baseline = getLineBounds(lastLineIndex, mRect);
      if (getMeasuredHeight() == getLayout().getHeight()) {
        result = mRect.bottom - (baseline + layout.getPaint().getFontMetricsInt().descent);
      }
    }
    return result;
  }
}

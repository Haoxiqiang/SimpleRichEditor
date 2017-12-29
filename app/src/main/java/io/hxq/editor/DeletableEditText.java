package io.hxq.editor;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

public class DeletableEditText extends AppCompatEditText {

  public DeletableEditText(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public DeletableEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public DeletableEditText(Context context) {
    super(context);
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
}
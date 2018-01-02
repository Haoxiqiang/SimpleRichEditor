package io.hxq.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;

public class DelegateImage extends RelativeLayout {

  private RichTextEditor.Tale tale;

  public DelegateImage(Context context) {
    super(context);
    init(context);
  }

  public DelegateImage(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public DelegateImage(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    View.inflate(context, R.layout.editor_image, this);
  }

  public void setOnImageCloseListener(Object tag, OnClickListener listener) {
    View closeView = findViewById(R.id.image_close);
    closeView.setTag(tag);
    closeView.setOnClickListener(listener);
  }

  public void setTale(RichTextEditor.Tale tale) {
    this.tale = tale;

    ImageView imageView = findViewById(R.id.edit_imageView);

    Glide.with(getContext())
        .load(tale.path)
        .apply(RequestOptions.centerCropTransform().priority(Priority.HIGH))
        .into(imageView);
  }

  public RichTextEditor.Tale getTale() {
    return tale;
  }
}

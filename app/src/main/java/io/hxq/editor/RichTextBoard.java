package io.hxq.editor;

import android.content.Context;
import android.support.annotation.MainThread;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import java.util.ArrayList;
import java.util.List;

public class RichTextBoard extends LinearLayout {

    private OnClickListener mediaClickListener = new OnClickListener() {
        @Override public void onClick(View v) {
            Object tag = v.getTag();
            if (tag instanceof RichTextEditor.Tale) {
                int index = medias.indexOf(tag);
                if (index >= 0) {
                    //covert and start

                }
            }
        }
    };

    private List<RichTextEditor.Tale> medias = new ArrayList<>();

    public RichTextBoard(Context context) {
        this(context, null);
    }

    public RichTextBoard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RichTextBoard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(LinearLayout.VERTICAL);
    }

    private void insertText(RichTextEditor.Tale tale) {
        View board = View.inflate(getContext(), R.layout.board_text, null);
        TextView view = board.findViewById(R.id.text);
        view.setText(tale.txt);
        addView(board);
    }

    private void insertImage(RichTextEditor.Tale tale) {
        View board = View.inflate(getContext(), R.layout.board_image, null);
        ImageView view = board.findViewById(R.id.image);
        view.setTag(tale);
        view.setOnClickListener(mediaClickListener);

        Glide.with(getContext())
            .load(tale.path)
            .apply(RequestOptions.centerCropTransform().priority(Priority.HIGH))
            .into(view);
        addView(board);
    }

    @MainThread public void setTale(List<RichTextEditor.Tale> tales) {

        if (tales == null || tales.isEmpty()) {
            return;
        }
        removeAllViews();
        for (int i = 0; i < tales.size(); i++) {
            RichTextEditor.Tale tale = tales.get(i);
            if (RichTextEditor.Tale.Type.Text.equals(tale.type)) {
                insertText(tale);
            } else {
                medias.add(tale);
                insertImage(tale);
            }
        }
    }
}

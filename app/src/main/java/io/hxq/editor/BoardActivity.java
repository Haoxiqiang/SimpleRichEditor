package io.hxq.editor;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import java.util.ArrayList;

public class BoardActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        RichTextBoard textBoard = findViewById(R.id.board);
        ArrayList<RichTextEditor.Tale> data = getIntent().getParcelableArrayListExtra("Data");
        textBoard.setTale(data);
    }
}

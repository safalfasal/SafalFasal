package crop.iisc.project.croppestdetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONObject;

public class ResultActivity extends Activity{
    private String res;
    Handler h = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_main);
        try {
            JSONObject json_object = new JSONObject(getIntent().getStringExtra("information"));
            res = json_object.getString("response");

        }
        catch (Exception e){
            Toast.makeText(this, "something happened bad", Toast.LENGTH_SHORT).show();
            finishActivity(1);
        }
        final TextView result = findViewById(R.id.result);
        result.setText(res);

        final Intent i = new Intent(this, MainActivity.class);

        int TIME_OUT = 2000;
        h.postDelayed(new Runnable(){
            @Override
            public void run() {
                res = null;

                startActivity(i);
                finish();
                Runtime.getRuntime().gc();

            }
        }, TIME_OUT);

    }


}

package com.divtec.blatnoa.bulleaniveau;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final float HALF_BIAS = 0.5f;

    ImageView bubble;
    ImageView line;
    TextView xAxisField;
    TextView yAxisField;

    SensorManager sensorManager;
    Sensor accelerometer;
    SensorEventListener gyroListener;
    ArrayList<Float> xAxisValues = new ArrayList<>();
    ArrayList<Float> yAxisValues = new ArrayList<>();

    ConstraintLayout layout;
    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
    boolean layoutStarted = false;
    int DPI = 0;

    double moveRange;
    double xBias = 0;
    double yBias = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DPI = getResources().getDisplayMetrics().densityDpi;

        // Lock screen orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get the components
        bubble = findViewById(R.id.levelBubble);
        line = findViewById(R.id.levelLine);
        xAxisField = findViewById(R.id.xAxis);
        yAxisField = findViewById(R.id.yAxis);
        layout = findViewById(R.id.layout);

        // Get layout dependant values as soon as possible
        layout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                // Get the bubble movement limits
                double moveRangeInDP = layout.getWidth() - 2*yAxisField.getWidth();
                moveRange = moveRangeInDP / layout.getWidth();

                params = (ConstraintLayout.LayoutParams) bubble.getLayoutParams();

                layoutStarted = true;
            }
        });

        // Get sensor manager and from it the rotation sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Create a listener for the rotation sensor
        gyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(android.hardware.SensorEvent event) {
                if (layoutStarted) {
                    updateLevelIndicator(event);
                    updateAxisFields(event.values[0], event.values[1]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        // Register the listener
        sensorManager.registerListener(gyroListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * Updates the level indicators
     * @param event The sensor event
     */
    private void updateLevelIndicator(SensorEvent event) {
        updateBubblePosition(event);
        updateLine();
    }

    /**
     * Update the text fields with the current values
     * @param x The x value
     * @param y The y value
     */
    private void updateAxisFields (float x, float y) {
        xAxisField.setText("X: " + Math.round(x/9.81*100) + "%");
        yAxisField.setText("Y: " + Math.round(y/9.81*100) + "%");
    }

    /**
     * Update the bubble position
     * @param event The sensor event
     */
    private void updateBubblePosition(SensorEvent event) {
        // Save 10 previous readings
        xAxisValues.add(event.values[0]);
        yAxisValues.add(-event.values[1]);
        if (xAxisValues.size() > 10) {
            xAxisValues.remove(0);
        }
        if (yAxisValues.size() > 10) {
            yAxisValues.remove(0);
        }

        // Get the average of the last 10 values
        // To smooth out the movement
        double x = getArrayAverage(xAxisValues);
        double y = getArrayAverage(yAxisValues);

        // Calculate the new bubble position
        double newX = HALF_BIAS + x/10;
        double newY = HALF_BIAS + y/10;

        // Check if the new position is within the range
        if (newX > HALF_BIAS + moveRange/2f) {
            newX = HALF_BIAS + moveRange /2f;
        } else if (newX < HALF_BIAS - moveRange /2f) {
            newX = HALF_BIAS - moveRange /2f;
        }
        if (newY > HALF_BIAS + moveRange /2f) {
            newY = HALF_BIAS + moveRange /2f;
        } else if (newY < HALF_BIAS - moveRange /2f) {
            newY = HALF_BIAS - moveRange /2f;
        }

        xBias = newX;
        yBias = newY;

        params.horizontalBias = (float) xBias;
        params.verticalBias = (float) yBias;
        bubble.setLayoutParams(params);
    }

    /**
     * Update the line angle to match the bubble position
     */
    private void updateLine() {
        // Get the bubble position relative to the pivot point
        double x = xBias * layout.getWidth();
        double y = (yBias-HALF_BIAS) * layout.getHeight();

        // Calculate the line angle with tan-1
        double rotation = Math.toDegrees(Math.atan(y/x));

        // Set the line rotation
        line.setRotation((float) rotation);
        if (rotation > -1 && rotation < 1) { // If the rotation is close to 0
            // Set the line to 0 and color in green
            line.getDrawable().setTint(ContextCompat.getColor(this, R.color.green));
        } else {
            // Set the line to the calculated rotation and color in red
            line.getDrawable().setTint(ContextCompat.getColor(this, R.color.red));
        }

        // Set the line length calculated with the pythagorean theorem
        line.getLayoutParams().width = (int) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    /**
     * Get the average of an array
     * @param array The array to get the average of
     * @return The average of the array
     */
    float getArrayAverage(ArrayList<Float> array) {
        float sum = 0;
        for (float current : array) {
            sum += current;
        }
        return sum / array.size();
    }

}
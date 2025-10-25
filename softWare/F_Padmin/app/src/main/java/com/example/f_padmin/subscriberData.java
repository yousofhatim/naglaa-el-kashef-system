package com.example.f_padmin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class subscriberData extends AppCompatActivity {
    private String mainCollection = "1";

    private DatabaseReference databaseRefer;
    private LinearLayout linearLayoutOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriber_data);

        databaseRefer = FirebaseDatabase.getInstance().getReference().child(mainCollection).child("orders");
        linearLayoutOrders = findViewById(R.id.linear_layout_orders);

        databaseRefer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                linearLayoutOrders.removeAllViews(); // Clear previous views

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String kidId = snapshot.getKey();
                    boolean order = snapshot.child("order").getValue(Boolean.class);

                    if (order) {
                        // Fetch kidName from another path
                        DatabaseReference kidNameRef = FirebaseDatabase.getInstance().getReference()
                                .child(mainCollection)
                                .child("data")
                                .child(kidId)
                                .child("kidName");

                        kidNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String kidName = dataSnapshot.getValue(String.class);

                                // Create a horizontal LinearLayout for each kid's order
                                LinearLayout horizontalLayout = new LinearLayout(subscriberData.this);
                                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                layoutParams.setMargins(0, 10, 0, 10);
                                horizontalLayout.setLayoutParams(layoutParams);

                                // Create a TextView to display kid's name
                                TextView textView = new TextView(subscriberData.this);
                                textView.setText(kidName);
                                textView.setTextSize(20);
                                textView.setPadding(20, 20, 20, 20);
                                horizontalLayout.addView(textView);

                                // Create a Button next to the TextView
                                Button button = new Button(subscriberData.this);
                                button.setText("تم التسليم");
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Change order status to false
                                        snapshot.getRef().child("order").setValue(false);
                                    }
                                });
                                horizontalLayout.addView(button);

                                // Add the horizontal layout to the main vertical layout
                                linearLayoutOrders.addView(horizontalLayout);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e("subscriberData", "Failed to read kidName value.", databaseError.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("subscriberData", "Failed to read value.", databaseError.toException());
            }
        });
    }
}

package noisy.desibel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    Activity activity;
    ArrayList<Item> items = new ArrayList<>();
    int item_layout;

    public RecyclerAdapter(Activity activity, int item_layout) {
        this.activity = activity;
        this.item_layout = item_layout;
    }

    public void add(String place, String noisy, String time) {
        Item item = new Item(place, noisy, time);

        items.add(0, item);
    }

    public void removeAll() {
        items.clear();
    }

    public String getPlace(int index) {
        return items.get(index).getPlace();
    }

    private void setAnimation(View viewToAntimate) {
        //if(position==0) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.ani);
        viewToAntimate.startAnimation(animation);
        //}
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cardview, null);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Item item = items.get(position);

        holder.place.setText(item.getPlace());
        holder.noisy.setText(item.getNoisy());
        holder.time.setText(item.getTime());
        holder.cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(context, item.getPlace(), Toast.LENGTH_SHORT).show();

                try {
                    PHPRequest request = new PHPRequest("http://jun3028.cafe24.com/user_signup/delete_user_information.php");
                    String result = request.PhPtest3(item.getPlace());
                    if (result.equals("1 record deleted")) {
                        Toast toast = Toast.makeText(activity, "장소를 입력해주세요", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.setView(activity.getLayoutInflater().inflate(R.layout.toast_view4, null));
                        toast.show();
                    } else {
                        Toast.makeText(activity, "오류", Toast.LENGTH_SHORT).show();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }


            }
        });
        setAnimation(holder.itemView);
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView place, noisy, time;
        CardView cardview;

        public ViewHolder(View itemView) {
            super(itemView);
            place = (TextView) itemView.findViewById(R.id.placeText);
            noisy = (TextView) itemView.findViewById(R.id.nosiyText);
            time = (TextView) itemView.findViewById(R.id.timeText);
            cardview = (CardView) itemView.findViewById(R.id.cardview);
        }
    }
}



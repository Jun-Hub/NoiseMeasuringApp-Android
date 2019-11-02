package noisy.desibel;

public class Item {
    String place, noisy, time;

    String getPlace() {
        return this.place;
    }
    String getNoisy() {
        return this.noisy;
    }
    String getTime() {
        return this.time;
    }

    Item(String place, String noisy, String time) {
        this.place = place;
        this.noisy = noisy;
        this.time = time;
    }
}

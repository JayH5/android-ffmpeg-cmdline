

import android.os.Parcel;
import android.os.Parcelable;

public class StoryTransition implements Parcelable {
	private int mPosition;
	private long mTime;
	
	StoryTransition(int position, long time) {
		mPosition = position;
		mTime = time;
	}
	
	StoryTransition(Parcel in) {
		mPosition = in.readInt();
		mTime = in.readLong();
	}
	
	public int getPosition() {
		return mPosition;
	}
	
	public long getTime() {
		return mTime;
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(mPosition);
		out.writeLong(mTime);
	}
	
	public static final Parcelable.Creator<StoryTransition> CREATOR
			= new Parcelable.Creator<StoryTransition>() {
		public StoryTransition createFromParcel(Parcel in) {
			return new StoryTransition(in);
		}
		
		public StoryTransition[] newArray(int size) {
			return new StoryTransition[size];
		}
	};
}
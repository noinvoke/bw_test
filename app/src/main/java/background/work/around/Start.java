package background.work.around;

import android.content.*;

public final class Start {    

    public static void RunService(Context context) {
        try{          
        Intent intent = new Intent("background.work.around.ALARM");
        intent.setPackage(context.getPackageName());            
        context.sendBroadcast(intent); 
        context.sendOrderedBroadcast(intent, null);    
        } catch(Throwable t) {}
    }    

}

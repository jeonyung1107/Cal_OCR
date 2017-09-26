package com.coffdope.jeon.cal_ocr;

import android.content.Intent;
import android.provider.CalendarContract;

import java.util.Calendar;

/**
 * Created by jeon on 17. 9. 10.
 */
// TODO: 17. 9. 26 should be implemented in different way to meke it easy
public class Calendar_activity {
    public Intent insert_event(int year, int month, int day, int time, int minute,String description){
        Calendar beginTime = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        beginTime.set(year, month, day, time, minute);
        end.set(year, month, day, time, minute);
        end.add(Calendar.DATE, 1);
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY,true)
                .putExtra(CalendarContract.Events.TITLE, "한 일")
                .putExtra(CalendarContract.Events.DESCRIPTION, description);
        return intent;
    }
}

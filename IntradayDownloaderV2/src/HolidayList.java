import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

public class HolidayList {
	public static void main(String args[])
	{
		Set<String> holidays = getHolidays(2018);
		DAO dao = new DAO();
		dao.storeHolidays(holidays, "NSE");
	}
	
	static Set<String> getHolidays(int thisYear)
	{
		Set<String> datesSet = new HashSet<String>();
		int[] years = {thisYear};
		int[] months = {0,1,2,3,4,5,6,7,8,9,10,11};
		String[] mktHolidays = {"2018-01-26","2018-02-13","2018-03-02","2018-03-29","2018-03-30","2018-05-01","2018-08-15","2018-08-22","2018-09-13","2018-09-20","2018-10-02","2018-10-18","2018-11-07","2018-11-08","2018-11-23","2018-12-25"};
		for(String holiday : mktHolidays)
		{
			datesSet.add(holiday);
		}
		for (int year : years) {
			for(int month : months)
			{
				Calendar cal = new GregorianCalendar(year, month, 1);
				do {
				    // get the day of the week for the current day
				    int day = cal.get(Calendar.DAY_OF_WEEK);
				    // check if it is a Saturday or Sunday
				    if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
				        String monthToPrint = (""+(month+1));
				        if(monthToPrint.length()==1)
				        {
				        	monthToPrint = "0"+monthToPrint;
				        }
				        String dayToPrint = ""+cal.get(Calendar.DAY_OF_MONTH);
				        if(dayToPrint.length()==1)
				        {
				        	dayToPrint = "0"+dayToPrint;
				        }
				        String holiday = year+"-"+monthToPrint+"-"+dayToPrint;
				        datesSet.add(holiday);
				    }
				    // advance to the next day
				    cal.add(Calendar.DAY_OF_YEAR, 1);
				}  while (cal.get(Calendar.MONTH) == month);
			}
		}		
		return datesSet;
	}
	
}

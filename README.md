## Roameo - your call for a healthier life


To quote myself:

> Roameo is the one missing fitness tracker for the physical inactive. Is roaming through the rooms like a maniac while being on the phone the only workout you get? Roameo will track your step count so you can share it with the world. Don't miss out anymore to tell your friends about your awesome sportiness!  
>  
> Roameo will detect incoming and outgoing phone calls and utilizes the phone's step count sensor to keep track of your walking around. All call sessions are individually accessible for minute by minute step count break down and can be shared to social media and messaging apps on your phone - or optionally uploaded to Google Fit. A weekly summary of all call sessions and the daily achieved step count, session duration and average pace (i.e. steps per minute) is graphically displayed for you.
Session exporter "x call session(s)" 


### Links

[Official website](http://roameo.craplab.fi)  
[Project website](http://sgreg.craplab.fi/projects/roameo)  
~~Google Play~~ [defunct]


### Features

* Count steps for every phone call and record it as a session in an internal database
* Display
  * summary statistics for all recorded sessions available
  * statistics for each week graphically summarized (step count, duration, pace (steps per minute)) and list the individual session for each day in the week
  * statistics for each individual session: step, count, duration, pace, bar graph to break down the steps for each minute either absolute or as deviation from the session's average value
  * Upload individual call sessions to Google Fit
  * Share individual call sessions to messaging and social media apps (calling the apps directly, so you'll need them installed)
* **1.1.0**: Display summary statistics for each week
* **1.2.0**: Export recorded call sessions as JSON

### Version History

#### 1.2.0

* New feature: Session Exporter
  * Export internally stored call sessions to JSON file
  * Select start date and date through dialog
  * Select to include the call session's phone numbers or not
  
#### 1.1.0

* New feature: Weekly Summary
  * Add summary dialog for each week individually
  * Accessible through the *Statistics* view
  * Containing same information than the main *Summary* view
  
#### 1.0.1

* Bug fix: Week offset calculation couldn't handle change of the year

#### 1.0.0

* Initial Release with all features planned for the app in the first place
  * Detect incoming and outgoing phone calls
  * Record step count with timestamp while call is ongoing
  * Convert timestamp information to minute-by-minute statistics
  * Write all phone call information to database
  * Show step count, duration, pace (steps/minute) and a minute-by-minute graph of walked steps for each phone call
  * Show weekly summary graph of step count, duration and pace, with individual session information accessible through it
  * Add optional connection with Google Fit to upload individual sessions to it (only manual, no automatic upload for privacy reasons)
  * Share individual sessions through other installed apps
  * Settings dialog for general behavior and privacy options


### Miscellaneous

#### Git repository history

Because of ..uhm, reasons, there is no git history before version 1.2.0 available here.  
Sorry about that.


#### Google Fit API

Note, if you're planning to build your own `.apk` from this code, you won't be able to use any of the Google Fit related functionality out of the box. The Google Fit API authenticates applications through the keystore fingerprints, so you need to set this up for your own fingerprints. See [Google Fit documentation](https://developers.google.com/fit/android/get-api-key) for more information on that.


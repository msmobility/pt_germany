# Title     : TODO
# Objective : TODO
# Created by: carlloga
# Created on: 27-Mar-20

library(readr)
library(dplyr)

gtfs_1_folder = "input/gtfs/db/"
gtfs_2_folder = "input/gtfs/rb/"
gtfs_folder = "input/gtfs/db_rb/"

files_gtfs = c("agency.txt", "calendar.txt", "feed_info.txt",
"routes.txt", "stop_times.txt", "stops.txt", "trips.txt")

for (file_gtfs in files_gtfs){
    file1 = read.csv(paste(gtfs_1_folder, file_gtfs, sep = ""))
    file2 = read.csv(paste(gtfs_2_folder, file_gtfs, sep = ""))
    file = file1 %>% bind_rows(file2)
    write.csv(file,paste(gtfs_folder, file_gtfs, sep = ""), row.names = F )
}





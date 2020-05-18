pacman::p_load(readr, dplyr, ggplot2,sf,tmap)


mode = "all"


all_tt = paste("c:/models/transit_germany/output/skims/",mode,"/pt_traveltimes.csv.gz", sep  ="")
all_train_share = paste("c:/models/transit_germany/output/skims/",mode,"/pt_trainshare_bytime.csv.gz", sep  ="")

tt_skim = read_csv2(gzfile(all_tt))



if (mode != "bus"){
  
  train_share_skim = read_csv2(gzfile(all_train_share))
  # tt_skim = tt_skim %>% merge(train_share_skim, by =  c("FROM", "TO"))
  
  tt_skim = tt_skim %>% bind_cols(train_share_skim)
  tt_skim = tt_skim %>% 
    mutate(validation_from = if_else(FROM == FROM1, 0, 1)) %>% 
    mutate(validation_to = if_else(TO == TO1, 0, 1)) %>% 
    mutate(validation = validation_from + validation_to) 
  
  sum(tt_skim$validation) 
  
  tt_skim = tt_skim %>% filter(VALUE1 != "NaN" , VALUE1 != "0.0")
  
  tt_skim = tt_skim %>% select(FROM, TO, VALUE)
}

writing = F
if (writing){
  write_csv(tt_skim, gzfile(paste("c:/models/transit_germany/output/skims/",mode, "/", mode, "_traveltimes.csv.gz", sep = "")))
}
#some code to validate the matrices


from_muc = tt_skim %>% filter(FROM == 3363) %>% mutate(tt_min = as.numeric(VALUE)/60)

from_muc = from_muc %>% filter(!is.na(tt_min), !is.infinite(tt_min))


#ggplot(from_muc, aes(x=tt_min)) + geom_histogram()

muc_shp = st_read("c:/models/transit_germany/input/zones/zones.shp")

muc_shp = muc_shp %>% left_join(from_muc, by = c("id"="TO"))

p =  tm_basemap(leaflet::providers$CartoDB) 

p = p + tm_shape(muc_shp, "time from 3499") +
  tm_polygons(alpha = 0.8, "tt_min", border.alpha = 0, style = "quantile")

tmap_leaflet(p)

setwd("C:/Git-data/Afstuderen/method_selection")
require(ggplot2)

ltr_data = read.csv("map_results.csv")
temp <- ltr_data[,c(2:(length(ltr_data)-2))]
winnum <- apply(temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
winnum <- data.frame(winnum)
ideal_winnum <- apply(winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
summed_winnum <- apply(winnum, 1, function(x) sum(x,na.rm=TRUE))
summed_ideal_winnum <- apply(ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))

norm_winnum <- summed_winnum/summed_ideal_winnum

nr_measurements <- apply(temp,1,function(x) sum(!is.na(x)))

output <- data.frame("Method" = ltr_data[,1], "Normalised_winning_number" = norm_winnum, "Nr_measurements"=nr_measurements)

# add ID's to output
output$id <- seq_len(nrow(output))
nc <- ncol(output)
output <- output[, c(nc, 1:(nc-1))]

# plot graph
ggplot(output, aes(Nr_measurements,Normalised_winning_number)) + geom_point(size=2) + 
  geom_text(aes(label=id), hjust=-0.5, vjust=-0.5) + 
  theme_grey(base_size=12, base_family="") + 
  scale_x_continuous("# of datasets evaluated on", expand = c(0,0), breaks=c(0,2,4,6,8,10,12)) +
  scale_y_continuous("Normalised Winning Number", expand = c(0,0), breaks=c(0,0.2,0.4,0.6,0.8,1.0)) +
  expand_limits(x = c(0,13.35), y = c(0,1.03))

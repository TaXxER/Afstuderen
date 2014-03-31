setwd("C:/Git-data/Afstuderen/method_selection")
require(ggplot2)


### MAP ###
map_data = read.csv("map_results.csv")
temp <- map_data[,c(2:(length(map_data)-2))]
map_winnum <- apply(temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
map_winnum <- data.frame(map_winnum)
map_ideal_winnum <- apply(map_winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
map_summed_winnum <- apply(map_winnum, 1, function(x) sum(x,na.rm=TRUE))
map_summed_ideal_winnum <- apply(map_ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))
map_norm_winnum <- map_summed_winnum/map_summed_ideal_winnum
map_nr_measurements <- apply(temp,1,function(x) sum(!is.na(x)))
map_output <- data.frame("Method" = map_data[,1], "Normalised_winning_number" = map_norm_winnum, "Nr_measurements"= map_nr_measurements)

# add ID's to output
map_output$id <- seq_len(nrow(map_output))
nc <- ncol(map_outputoutput)
map_output <- map_outputoutput[, c(nc, 1:(nc-1))]


### NDCG@5 ###
ndcg5_data = read.csv("ndcg5_results.csv")
temp <- ndcg5_data[,c(2:(length(ndcg5_data)-2))]
ndcg5_winnum <- apply(temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
ndcg5_winnum <- data.frame(ndcg5_winnum)
ndcg5_ideal_winnum <- apply(ndcg5_winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
ndcg5_summed_winnum <- apply(ndcg5_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg5_summed_ideal_winnum <- apply(ndcg5_ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg5_norm_winnum <- ndcg5_summed_winnum/ndcg5_summed_ideal_winnum
ndcg5_nr_measurements <- apply(temp,1,function(x) sum(!is.na(x)))
ndcg5_output <- data.frame("Method" = ndcg5_data[,1], "Normalised_winning_number" = ndcg5_norm_winnum, "Nr_measurements"= ndcg5_nr_measurements)

# add ID's to output
ndcg5_output$id <- seq_len(nrow(ndcg5_output))
nc <- ncol(ndcg5_output)
ndcg5_output <- ndcg5_output[, c(nc, 1:(nc-1))]


### NDCG@10 ###
ndcg10_data = read.csv("ndcg10_results.csv")
temp <- ndcg10_data[,c(2:(length(ndcg10_data)-2))]
ndcg10_winnum <- apply(temp, 2, function(x) sapply(x, function(y) if(is.na(y)){NA}else{sum(x[!is.na(x)]<y)}))
ndcg10_winnum <- data.frame(ndcg10_winnum)
ndcg10_ideal_winnum <- apply(ndcg10_winnum, 2, function(x) sapply(x, function(y) if(is.na(y)){0}else{if(length(x[!is.na(x)]) == 0){0}else{max(x[!is.na(x)])}}))
ndcg10_summed_winnum <- apply(ndcg10_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg10_summed_ideal_winnum <- apply(ndcg10_ideal_winnum, 1, function(x) sum(x,na.rm=TRUE))
ndcg10_norm_winnum <- ndcg10_summed_winnum/ndcg10_summed_ideal_winnum
ndcg10_nr_measurements <- apply(temp,1,function(x) sum(!is.na(x)))
ndcg10_output <- data.frame("Method" = ndcg10_data[,1], "Normalised_winning_number" = ndcg10_norm_winnum, "Nr_measurements" = ndcg10_nr_measurements)

# add ID's to output
ndcg10_output$id <- seq_len(nrow(ndcg10_output))
nc <- ncol(ndcg10_output)
ndcg10_output <- ndcg10_output[, c(nc, 1:(nc-1))]

### COMBINED ###
# TODO #

### PLOT ###
### ADJUST HERE ###
# plot graph
ggplot(ndcg5_output, aes(Nr_measurements,Normalised_winning_number)) + geom_point(size=2) + 
  geom_text(aes(label=id), hjust=-0.5, vjust=-0.5) + 
  theme_grey(base_size=12, base_family="") + 
  scale_x_continuous("# of datasets evaluated on", expand = c(0,0), breaks=c(0,2,4,6,8,10,12,14, 16)) +
  scale_y_continuous("Normalised Winning Number", expand = c(0,0), breaks=c(0,0.2,0.4,0.6,0.8,1.0)) +
  expand_limits(x = c(0,16.35), y = c(0,1.03))
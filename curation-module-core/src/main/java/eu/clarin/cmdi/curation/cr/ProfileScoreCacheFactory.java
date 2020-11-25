package eu.clarin.cmdi.curation.cr;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import eu.clarin.cmdi.curation.entities.CMDIProfile;
import eu.clarin.cmdi.curation.report.CMDIProfileReport;

class ProfileScoreCacheFactory{
	
	static final long HOUR_IN_NS = 3600000000000L;
	
	private static final Logger logger = LoggerFactory.getLogger(ProfileScoreCacheFactory.class);
	
	public static LoadingCache<ProfileHeader, Double> createScoreCache(boolean isPublicProfilesCache){
		return isPublicProfilesCache?
		
		CacheBuilder.newBuilder()
				.concurrencyLevel(4)
				.build(new ScoreCacheLoader())
		:
		CacheBuilder.newBuilder()
			.concurrencyLevel(4)
			.expireAfterWrite(8, TimeUnit.HOURS)//keep non public profiles 8 hours in cache
			.ticker(new Ticker() { @Override public long read() { return 9 * HOUR_IN_NS; } }) //cache tick 9 hours
			.build(new ScoreCacheLoader());		
	}
	
	
	private static class ScoreCacheLoader extends CacheLoader<ProfileHeader, Double>{		
		
		/*
		 * process profile and create new Score object by aggregating individual scores
		 */
		
		@Override
		public Double load(ProfileHeader header) throws Exception{

			CMDIProfile profile = new CMDIProfile(header.getSchemaLocation(), header.getCmdiVersion());
			logger.trace("Calculating and caching score for {}", profile);

			CMDIProfileReport report = profile.generateReport();

			
			if(CRService.PROFILE_MAX_SCORE.equals(Double.NaN)){
				CRService.PROFILE_MAX_SCORE = report.maxScore;
			}
			
			return report.score;
			
		}
		
	}

}

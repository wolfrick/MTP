package config;

public class BranchPredictorConfig {
	public int PCBits;
	public int BHRsize;
	public int saturating_bits;
	public BP predictorMode;
	
	public static enum BP {
		NoPredictor, PerfectPredictor, AlwaysTaken, AlwaysNotTaken, Tournament, Bimodal, GShare, GAg, GAp, PAg, PAp,TAGE
	}
}

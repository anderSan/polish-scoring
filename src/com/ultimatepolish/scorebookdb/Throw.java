package com.ultimatepolish.scorebookdb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.widget.ImageView;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.ultimatepolish.polishscorebook.R;

public class Throw implements Comparable<Throw>{
	public static final String GAME_ID = "gameId";
	public static final String THROW_INDEX = "throwIdx";

	@DatabaseField(generatedId=true)
	private long id;
	
	@DatabaseField(canBeNull=false, uniqueCombo=true)
	private int throwIdx;

	@DatabaseField(canBeNull=false, uniqueCombo=true)
	private long gameId;

	@DatabaseField(canBeNull=false)
	private long offensivePlayerId;

	@DatabaseField(canBeNull=false)
	private long defensivePlayerId;
	
	@DatabaseField(canBeNull=false)
	private Date timestamp;

	@DatabaseField(canBeNull=false)
	private int throwType;

	@DatabaseField(canBeNull=false)
	private int throwResult;

	@DatabaseField
	private int deadType = 0;
	
	@DatabaseField
	public boolean isTipped = false;
	
	@DatabaseField
	public boolean isGoaltend = false;
		
	@DatabaseField
	public boolean isDrinkHit = false;
	
	@DatabaseField
	public boolean isOnFire = false;

	@DatabaseField
	public boolean isLineFault = false;
	
	@DatabaseField
	public boolean isOffensiveDrinkDropped = false;
	
	@DatabaseField
	public boolean isOffensivePoleKnocked = false;
	
	@DatabaseField
	public boolean isOffensiveBottleKnocked = false;
	
	@DatabaseField
	public boolean isOffensiveBreakError = false;
	
	@DatabaseField
	public boolean isDefensiveDrinkDropped = false;
	
	@DatabaseField
	public boolean isDefensivePoleKnocked = false;
	
	@DatabaseField
	public boolean isDefensiveBottleKnocked = false;
	
	@DatabaseField
	public boolean isDefensiveBreakError = false;
	
	@DatabaseField
	private int initialOffensivePlayerScore = 0;

	@DatabaseField
	private int initialDefensivePlayerScore = 0;
	
	Throw(){}
	
	public Throw(int throwIdx, long gameId, long offensivePlayerId, long defensivePlayerId, Date timestamp,
			int throwType, int throwResult) {
		super();
		this.throwIdx = throwIdx;
		this.gameId = gameId;
		this.offensivePlayerId = offensivePlayerId;
		this.defensivePlayerId = defensivePlayerId;
		this.timestamp = timestamp;
		this.throwType = throwType;
		this.throwResult = throwResult;
	}
	
	public Throw(int throwIdx, long gameId, long offensivePlayerId, long defensivePlayerId, Date timestamp) {
		super();
		this.throwIdx = throwIdx;
		this.gameId = gameId;
		this.offensivePlayerId = offensivePlayerId;
		this.defensivePlayerId = defensivePlayerId;
		this.timestamp = timestamp;
        this.throwType = ThrowType.NOT_THROWN;
	}
	
    public static Dao<Throw, Long> getDao(Context context){
		DatabaseHelper helper = new DatabaseHelper(context);
		Dao<Throw, Long> d = null;
                try {
                        d = helper.getThrowDao();
                }
                catch (SQLException e){
                        throw new RuntimeException("Couldn't get dao: ", e);
                }
		return d;
	}

	public HashMap<String, Object> getQueryMap(){
        HashMap<String,Object> m = new HashMap<String,Object>();
        m.put(Throw.THROW_INDEX, getThrowIdx());
        m.put(Throw.GAME_ID, getGameId());
        return m;
	}

	public void setInitialScores(Throw previousThrow){
		int[] scores = previousThrow.getFinalScores();
		setInitialDefensivePlayerScore(scores[0]);
		setInitialOffensivePlayerScore(scores[1]);
	}

	public void setInitialScores(){
		setInitialDefensivePlayerScore(0);
		setInitialOffensivePlayerScore(0);
	}

	public int[] getFinalScores(){
		int[] diff = getScoreDifferentials();
		int[] finalScores = {initialOffensivePlayerScore + diff[0], 
				initialDefensivePlayerScore + diff [1]};
		return finalScores;
	}

	private int[] getScoreDifferentials(){
		int[] diffs = {0,0};
		switch (throwResult){
		case ThrowResult.NA:
			if (throwType == ThrowType.TRAP) {
				diffs[0] = -1;
			}
			break;
		case ThrowResult.DROP:
			if (!isLineFault) {
				switch (throwType){
					case ThrowType.STRIKE:
						if (!isDropScoreBlocked() && deadType == 0){
							diffs[0] = 1;
						}
						break;
					case ThrowType.POLE: 
					case ThrowType.CUP:
						if (!isTipped) {
							diffs[0] = 2;
							if (isGoaltend) {
								// if goaltended, an extra point for dropping disc
								diffs[0] += 1;
							}
						}
						break;
					case ThrowType.BOTTLE:
						if (!isTipped) {
							diffs[0] = 3;
							if (isGoaltend) {
								// if goaltended, an extra point for dropping disc
								diffs[0] += 1;
							}
						}
						break;
					default:
						break;
				}
			}
			break;
		case ThrowResult.CATCH:
			if (!isLineFault) {
				switch (throwType){
					case ThrowType.POLE: 
					case ThrowType.CUP:
						if (!isTipped) {
							if (isGoaltend) {
								// if goaltended, award points for hit
								diffs[0] = 2;
							}
						}
						break;
					case ThrowType.BOTTLE:
						if (!isTipped) {
							if (isGoaltend) {
								// if goaltended, award points for hit
								diffs[0] = 3;
							}
						}
						break;
					default:
						break;
				}
			}
			break;
		case ThrowResult.STALWART:
			diffs[1] = 1;
			break;
		case ThrowResult.BROKEN:
			if (!isLineFault) {
				diffs[0] = 20;
			}
			break;
		default:
			break;
		}
		
		// extra points for other modifiers
		if (isDrinkHit){
			diffs[1] -= 1;
		}
		if (isOffensiveDrinkDropped){
			diffs[0] -= 1;
		}
		if (isOffensivePoleKnocked){
			diffs[1] += 2;
		}
		if (isOffensiveBottleKnocked){
			diffs[1] += 3;
		}
		if (isOffensiveBreakError){
			diffs[1] += 20;
		}
		if (isDefensiveDrinkDropped){
			diffs[1] -= 1;
		}
		if (isDefensivePoleKnocked){
			diffs[0] += 2;
		}
		if (isDefensiveBottleKnocked){
			diffs[0] += 3;
		}
		if (isDefensiveBreakError){
			diffs[0] += 20;
		}
		
		return diffs;
	}

	private boolean isDropScoreBlocked(){
		boolean isBlocked = false;
		int oScore = initialOffensivePlayerScore;
		int dScore = initialDefensivePlayerScore;
		if (oScore>=10 && dScore<oScore){
			isBlocked = true;
		}
		return isBlocked;
	}
	
	public String getSpecialString(){
		String s = "";
		
		if (isLineFault){
			s += "lf.";
		}
		
		if (isDrinkHit){
			s += "d.";
		}
		
		if (isGoaltend){
			s += "gt.";
		}
		
		int og = 0;
		// technically drink drops are -1 for player instead of +1 for opponent,
		// but subtracting the value for display purposes would be more confusing
		// this is really displaying the resulting differential due to og
		if (isOffensiveDrinkDropped) {og += 1;}
		if (isOffensivePoleKnocked) {og += 2;} 
		if (isOffensiveBottleKnocked) {og += 3;}
		if (isOffensiveBreakError) {og += 20;}
		if (og > 0){
			s += "og" + String.valueOf(og) + '.';
		}
		
		int err = 0;
		// same as for og
		if (isDefensiveDrinkDropped) {err += 1;}
		if (isDefensivePoleKnocked) {err += 2;} 
		if (isDefensiveBottleKnocked) {err += 3;}
		if (isDefensiveBreakError) {err += 20;}
		if (err > 0){
			s += "e" + String.valueOf(err) + '.';
		}
				
		if (s.length()==0){
			s = "--";
		} else {
			// pop the last '.' off the end of the string
			s = s.substring(0, s.length()-1);
		}
		return s;
	}

	public void setThrowDrawable(ImageView iv){
		List<Drawable> boxIconLayers = new ArrayList<Drawable>();
		
		if (!getIsValid()) {
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_badthrow));
		}
		switch (throwType) {
		case ThrowType.BOTTLE:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_bottle));
			break;
		case ThrowType.CUP:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_cup));
			break;
		case ThrowType.POLE:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_pole));
			break;
		case ThrowType.STRIKE:
			if (throwResult == ThrowResult.CATCH) {
				boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_strike));
			}
			break;
		case ThrowType.BALL_HIGH:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_high));
			break;
		case ThrowType.BALL_RIGHT:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_right));
			break;
		case ThrowType.BALL_LOW:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_low));
			break;
		case ThrowType.BALL_LEFT:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_left));
			break;
		case ThrowType.SHORT:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_short));
			break;
		case ThrowType.TRAP:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_trap));
			break;
		case ThrowType.TRAP_REDEEMED:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_trap));
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_over_drop));
			break;
		case ThrowType.NOT_THROWN:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_notthrown));
			break;
		case ThrowType.FIRED_ON:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_under_firedon));
			break;
		default:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_oops));
			break;
		}
		
		switch (throwResult) {
		case ThrowResult.DROP:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_over_drop));
			break;
		case ThrowResult.STALWART:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_over_stalwart));
			break;
		case ThrowResult.BROKEN:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_over_break));
			break;
		}
		
		switch (deadType) {
		case DeadType.HIGH:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_dead_high));
			break;
		case DeadType.RIGHT:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_dead_right));
			break;
		case DeadType.LOW:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_dead_low));
			break;
		case DeadType.LEFT:
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_dead_left));
			break;
		}
		
		if (isOnFire) {
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_over_fire));
		}
		if (isTipped) {
			boxIconLayers.add(iv.getResources().getDrawable(R.drawable.bxs_over_tipped));
		}
		
		iv.setImageDrawable(new LayerDrawable(boxIconLayers.toArray(new Drawable[0])));
	}
	
	public boolean getIsValid() {
		boolean valid = true;
		
		if (isOnFire) {
			if (throwResult != ThrowResult.NA && throwResult != ThrowResult.BROKEN) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Throw result must be NA or Broken");
			}
		}
		switch (throwType) {
		case ThrowType.BALL_HIGH:
		case ThrowType.BALL_RIGHT:
		case ThrowType.BALL_LOW:
		case ThrowType.BALL_LEFT:
		case ThrowType.STRIKE:
			if (deadType != 0 && isDrinkHit){
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), drinkHit must be on a live throw");
			} else if (isGoaltend || isTipped) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Goaltending and tipped dont make sense for SHRLL throw types");
			}
			
			switch (throwResult) {
			case ThrowResult.DROP:
			case ThrowResult.CATCH:
				break;
			default:
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Result for SHRLL throws must be a drop or catch");
				break;
			}
			
			break;
			
		case ThrowType.POLE:
		case ThrowType.CUP:
		case ThrowType.BOTTLE:
			if (isDrinkHit) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), drink hits cant occur with PCB hits");
			} else if (isTipped && isGoaltend) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), PCB throws cant be tipped and goaltended simultaneously");
			} else if (deadType != 0 && isGoaltend) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Dead throws cannot be goaltended");
			} else if (throwResult == ThrowResult.NA) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), PCB throws cannot have NA result");
			} else if (isTipped && throwResult == ThrowResult.STALWART) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Not possible to stalwart on a tip");
			} else if (isGoaltend && throwResult == ThrowResult.STALWART) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Not possible to stalwart and goaltend");
			} else if (isTipped && throwResult == ThrowResult.BROKEN) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Cannot break on a tip");
			} else if (isGoaltend && throwResult == ThrowResult.BROKEN) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Cannot break on a goaltend");
			}
			
			break;
			
		case ThrowType.TRAP:
		case ThrowType.TRAP_REDEEMED:
		case ThrowType.SHORT:
			if (isGoaltend || isTipped || isDrinkHit) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Goaltend, tip, and drinkHit dont apply to trap or short");
			} else if (throwResult != ThrowResult.NA) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Trap and short can only have NA result");
			}
			
			break;
		case ThrowType.FIRED_ON:
			// fired_on is a dummy throw, so modifiers dont count and result must be NA
			// errors could potentially happen while returning the disc, so those are allowed
			if (isLineFault || isGoaltend || isTipped || isDrinkHit || deadType != 0) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Fired-on cannot be modified");
			} else if (throwResult != ThrowResult.NA) {
				valid = false;
				Log.i("Throw.db()", "getIsValid: (" + throwIdx + "), Fired-on must have NA result");
			}
			
			break;
		}
		return valid;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
        this.id = id;
	}

	public int getThrowIdx() {
		return throwIdx;
	}

	public void setThrowIdx(int throwIdx) {
        this.throwIdx = throwIdx;
	}
	
	public long getGameId() {
		return gameId;
	}

	public long getOffensivePlayerId() {
		return offensivePlayerId;
	}

	public long getDefensivePlayerId() {
		return defensivePlayerId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public int getThrowType() {
		return throwType;
	}

	public void setThrowType(int throwType) {
		this.throwType = throwType;
	}

	public int getThrowResult() {
		return throwResult;
	}

	public void setThrowResult(int throwResult) {
		this.throwResult = throwResult;
	}
	
	public int getDeadType() {
		return deadType;
	}

	public void setDeadType(int deadType) {
		this.deadType = deadType;
	}
	
	public int getInitialOffensivePlayerScore() {
		return initialOffensivePlayerScore;
	}

	public void setInitialOffensivePlayerScore(int initialOffensivePlayerScore) {
		this.initialOffensivePlayerScore = initialOffensivePlayerScore;
	}

	public int getInitialDefensivePlayerScore() {
		return initialDefensivePlayerScore;
	}

	public void setInitialDefensivePlayerScore(int initialDefensivePlayerScore) {
		this.initialDefensivePlayerScore = initialDefensivePlayerScore;
	}

	public int compareTo(Throw another) {
		if (throwIdx<another.throwIdx){
			return -1;
		}
		else if(throwIdx==another.throwIdx){
			return 0;
		}
		else{
			return 1;
		}
	}

	public static boolean isP1Throw(int throwIdx) {
		return throwIdx%2==0;
	}
	public static boolean isP1Throw(Throw t){
		return isP1Throw(t.getThrowIdx());
	}
	public boolean isP1Throw(){
		return isP1Throw(throwIdx);
	}	
}

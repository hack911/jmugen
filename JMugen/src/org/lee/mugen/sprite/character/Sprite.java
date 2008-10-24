package org.lee.mugen.sprite.character;

import static org.lee.mugen.util.Logger.log;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lee.mugen.core.StateMachine;
import org.lee.mugen.core.physics.PhysicsEngime;
import org.lee.mugen.input.MugenCommands;
import org.lee.mugen.io.IOUtils;
import org.lee.mugen.parser.air.AirParser;
import org.lee.mugen.sff.SffReader;
import org.lee.mugen.snd.Snd;
import org.lee.mugen.snd.SndReader;
import org.lee.mugen.sprite.base.AbstractSprite;
import org.lee.mugen.sprite.baseForParse.SpriteSFF;
import org.lee.mugen.sprite.cns.StateDef;
import org.lee.mugen.sprite.entity.PointF;
import org.lee.mugen.sprite.parser.CmdParser;
import org.lee.mugen.sprite.parser.CnsParse;
import org.lee.mugen.sprite.parser.Parser;
import org.lee.mugen.sprite.parser.Parser.GroupText;
import org.lee.mugen.util.BeanTools;
import org.lee.mugen.util.MugenRandom;


public class Sprite extends AbstractSprite implements Cloneable {
	public static SpriteDef parseSpriteDef(String def, String spriteId)
			throws Exception {

		SpriteDef spriteDef = new SpriteDef();
		String parentPath = new File(def).getParentFile().getAbsolutePath();
		spriteDef.setParentPath(parentPath);
		List<GroupText> groups = Parser
				.getGroupTextMap(org.apache.commons.io.IOUtils
						.toString(new FileInputStream(def)));

		for (GroupText grp : groups) {
			String section = grp.getSection();
			Map<String, String> kvs = grp.getKeyValues();
			for (String key : kvs.keySet()) {
				String value = kvs.get(key);

				Matcher m = Pattern.compile("(^.*pal|^.*st)([0-9]*)$").matcher(
						key);
				if (m.find()) {
					value = m.group(2) + "," + value;
					key = m.group(1);
					
				}
				// String[] tokens = ExpressionFactory.expression2Tokens(value);
				// Valueable[] vals = ExpressionFactory.evalExpression(tokens);
				// Object[] obs = ValueableUtils.convert(vals);
				try {
					BeanTools.setObject(spriteDef, section + "." + key, value);
				} catch (Exception e) {
					System.err.println(section + "." + key + " is on error with value " + value);
//					e.printStackTrace();
				}


			}
		}
		List<GroupText> grps = CnsParse.getCnsGroup(parentPath, spriteDef.getFiles()
				.getCns(), spriteDef.getFiles().getStcommon(), spriteDef
				.getFiles().getSt());
		spriteDef.setCnsGroups(grps);
		return spriteDef;
	}
	
	public void nextPal() {
		SpriteDef oneDef = StateMachine.getInstance().getSpriteDef(spriteId);
		if (pal + 1 < oneDef.getFiles().getPal().length - 1)
			changePal(pal + 1);
	}
	
	public void previousPal() {
		if (pal - 1 > 0)
			changePal(pal - 1);
	}
	
	public void roundPal() {
		SpriteDef oneDef = StateMachine.getInstance().getSpriteDef(spriteId);
		if (pal + 1 < oneDef.getFiles().getPal().length - 1)
			changePal(pal + 1);
		else
			changePal(0);
	}
	

//	private String[] getSuitablePalPath() {
//		String[] pals = definition.getFiles().getPal();
//		String[] result = new String[pals.length];
//		
//		for (int i = 0; i < pals.length; i++) {
//			StringTokenizer token = new StringTokenizer(pals[i], ",");
//			int id = Integer.parseInt(token.nextToken());
//			String palPath = token.nextToken();
//			
//			result[id-1] = palPath;
//		}
//		
//		return result;
//	}
	
	private void buildSpriteSff(int pal, boolean isReload) throws FileNotFoundException,
			IOException {

		byte[] tempsPal = null;
		Integer defaultPal = 0;
		if (definition.getInfo().getPal().getDefaults().length > 0)
			defaultPal = definition.getInfo().getPal().getDefaults()[0];
		if (pal != 0)
			defaultPal = pal + 1;
		else
			defaultPal = 0;
		String pathToAct = null;
		String sffPath = new File(definition.getParentPath(), definition
				.getFiles().getSprite()).getAbsolutePath();
		if (defaultPal - 1 > 0) {
			pathToAct = new File(definition.getParentPath(), definition.getFiles().getPal()[defaultPal - 1]).getAbsolutePath();

		}
		log("Copy Stream into memory");
		if (pathToAct != null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(new FileInputStream(pathToAct), bos);
			tempsPal = bos.toByteArray();
			byte[] tempsInvers = new byte[768];
			int count = 3;
			for (int i = 0; i < tempsInvers.length; i += count) {
				int c = 0;
				int cm = count - 1;
				for (int cc = 0; cc < count; cc++)
					tempsInvers[i + c++] = tempsPal[768 - i - cm-- - 1];

			}
			tempsPal = tempsInvers;
		}
		log("End Copy Stream into memory");

		log("Load SFFReader");

		SffReader sffReader = new SffReader(sffPath, tempsPal);
		log("End Load SFFReader");
		
		log("Load SpriteSff");

		SpriteSFF spriteSFF = new SpriteSFF(sffReader, true);
		log("End Load SpriteSff");
		
		SpriteSFF oldSff = getSpriteSFF();
		
		if (isReload) {
			oldSff.reload(spriteSFF);
		} else {
			setSpriteSFF(spriteSFF);
		}
		this.pal = pal;
		

		
	
		
	}
	public void changePal(int pal) {
		try {
			buildSpriteSff(pal, true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public float getXScale() {
		return getInfo().getSize().getXscale();
	}
	
	
	@Override
	public float getYScale() {
		return getInfo().getSize().getYscale();
	}
	protected List<MugenCommands> cmds;

	protected SpriteDef definition;

	protected SpriteCns info;

	private String spriteId;

	protected Snd spriteSnd;

	protected SpriteState spriteState;

	protected int pal;
	protected Sprite() {
	}
	
	public int getPal() {
		return pal;
	}

	private Integer tempPause = 0;
	@Override
	public void setPause(int pause) {
		tempPause = pause;
//		this.pause = pause;
	}
	public Sprite(String spriteId, SpriteDef spriteDef, int pal) {
		this(spriteId, spriteDef, pal, true);
	}

	public Sprite(String spriteId, SpriteDef spriteDef, int pal, boolean isLoadState) {
		try {
			definition = spriteDef;
			this.spriteId = spriteId;
			log("Load air data");
			AirParser airParser = new AirParser(new File(definition
					.getParentPath(), definition.getFiles().getAnim())
					.getAbsolutePath());
			log("End load air data");
			
			log("Load cns");
			info = new SpriteCns(spriteId);
			log("End load cns");
			
			log("Load build state");
			spriteState = new SpriteState(spriteId);
			if (isLoadState)
				CnsParse.buildSpriteInfo(spriteDef.getCnsGroups(), this, info,
					spriteState);
			log("End Load build state");
			
			log("Load anim");
			SpriteAnimManager sprAnimMng = new SpriteAnimManager(spriteId, airParser);
			setSprAnimMng(sprAnimMng);
			log("End Load anim");

			this.pal = pal;
			
			log("Load Sff");
			buildSpriteSff(pal, false);
			log("End Load Sff");
			
			log("Load Sound");
			if (definition.getFiles().getSound() != null && definition.getFiles().getSound().length() > 0)
				spriteSnd = SndReader.parse(new FileInputStream(new File(definition
						.getParentPath(), definition.getFiles().getSound())
						.getAbsolutePath()));
			log("End Load Sound");

			// cmds = parseCmd(definition.getFiles().getCmd());
			CmdParser.parse(new FileInputStream(new File(definition
					.getParentPath(), definition.getFiles().getCmd())), this);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Error in Parse Sprite ", e);
		}

	}


	@Override
	public Object clone() throws CloneNotSupportedException {
		Sprite cloneSprite = (Sprite) super.clone();
		
		SpriteAnimManager animManager = new SpriteAnimManager(spriteId, getSprAnimMng().getGroupSpriteMap());
		
		SpriteState spriteState = new SpriteState(spriteId);
		spriteState.setStatedefMap(getSpriteState().cloneStateNormalDef());
		spriteState.setNegativeStateSef(new ArrayList<StateDef>());
//		spriteState.setNegativeStateSef(getSpriteState().cloneNegativeState());
		
		SpriteCns spriteCns = (SpriteCns) getInfo().clone();
		
		cloneSprite.sprAnimMng = animManager;
		cloneSprite.spriteState = spriteState;
		cloneSprite.info = spriteCns;
		
		cloneSprite.cmds = cmds;
		return cloneSprite;
	}

	public List<MugenCommands> getCmds() {
		if (cmds == null)
			cmds = new ArrayList<MugenCommands>();
		return cmds;
	}

	/*
	 * ___|____
	 * 
	 * ________
	 * 
	 * 
	 */
	@Override
	public List<Rectangle> getCns1() {
		AnimElement imgSpr = getSprAnimMng().getCurrentImageSprite();
		if (imgSpr == null) {
			
			System.err.println("Action " + getSprAnimMng().getAction() + " n'existe pas ");
			return Collections.EMPTY_LIST;
		}
		return getCns(imgSpr.getAtacksRec());

	}

	@Override
	public List<Rectangle> getCns2() {
		AnimElement imgSpr = getSprAnimMng().getCurrentImageSprite();
		if (imgSpr == null)
			return Collections.EMPTY_LIST;
		return getCns(imgSpr.getCollisionsRec());

	}

	public SpriteDef getDefinition() {
		return definition;
	}

	public SpriteCns getInfo() {
		return info;
	}

	@Override
	public PointF getPosToDraw() {
		PointF pt = super.getPosToDraw();
		if (info.getShake().getTime() > 0) {
			pt.setX(pt.getX() + MugenRandom.getInstanceOf().nextInt(info.getShake().getAmpl()));
			pt.setY(pt.getY() + MugenRandom.getInstanceOf().nextInt(info.getShake().getAmpl()));
			
		} else {
			pt.setX(pt.getX());
			pt.setY(pt.getY());

		}
		pt.setX(pt.getX() + info.getSize().getDraw().getOffset().getX());
		pt.setY(pt.getY() + info.getSize().getDraw().getOffset().getY());
		return pt;
	}
	//	private long timeHitter;
//	public void setTimeHitter() {
//		timeHitter = linearTime;
//	}
//	public boolean canHit() {
//		return timeHitter != linearTime;
//	}
	@Override
	public int getPriority() {
		return getInfo().getSprpriority();
	}

	@Override
	public float getRealXPos() {
		return info.getXPos();
	}
	

	@Override
	public float getRealYPos() {
		return info.getYPos();
	}

	public String getSpriteId() {
		return spriteId;
	}

	public Snd getSpriteSnd() {
		return spriteSnd;
	}

	public SpriteState getSpriteState() {
		return spriteState;
	}

	public boolean isBindToOhterSprState() {
		return spriteState.isBindToOhterSprState();
	}

	@Override
	public boolean isFlip() {
		return info.isFlip();
	}

	@Override
	public void process() {

		getSprAnimMng().process();
		info.process();
		spriteState.process();
		PhysicsEngime.processSpritePhysics(this);
		info.decreaseScreenbound();
		
		getPalfx().decreaseTime();
		linearTime++;
		if (tempPause != null) {
			pause = tempPause;
			tempPause = null;
		}
		if (isPause()) {
			processPause();
//			return;
		}

		
	}

	public void setInfo(SpriteCns info) {
		this.info = info;
	}
	public void setSpriteId(String spriteId) {
		this.spriteId = spriteId;
		info.setSpriteId(spriteId);
		getSprAnimMng().setSpriteId(spriteId);
		spriteState.setSpriteId(spriteId);
	}
	
	public void setSpriteState(SpriteState spriteState) {
		this.spriteState = spriteState;
	}

	public Integer getTempPause() {
		return tempPause;
	}
	


}

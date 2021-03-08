package com.niton;

import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarResult;
import com.niton.parser.result.*;
import com.niton.parser.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;


public class ResultDisplay extends com.niton.media.visual.Canvas {

	int y_off = 0;
	private final SuperGrammarResult  res;
	private final GrammarReference    ref;
	@Getter
	@Setter
	private       List<AssignedToken> tokenList;
	private final Font                f = new Font(Font.MONOSPACED,
	                                               Font.PLAIN,
	                                               12);
	private       Map<String, Color>  tokenNames;
	private       Map<String, Color>  grammarNames;
	private       int                 x_off;

	public ResultDisplay(SuperGrammarResult res, GrammarReference ref) {
		this.res  = res;
		tokenList = res.join();
		this.ref  = ref;
		setFps(1);
		setLimitFrames(true);
		setFocusTraversalKeysEnabled(true);
		setFocusable(true);
		grabFocus();
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				tokenNames = new HashMap<>();
				for (AssignedToken tkn : tokenList) {
					tokenNames.put(tkn.name, new Color((int) (Math.random() * Integer.MAX_VALUE)));
				}
				grammarNames = new HashMap<>();
				for (String tkn : ref.grammarNames()) {
					grammarNames.put(tkn, new Color((int) (Math.random() * Integer.MAX_VALUE)));
				}
			}
		});
	}

	@Override
	public void paint(Graphics2D g, int i) {
		if(!isVisible())
			return;
		g.setFont(f);
		if (tokenNames == null) {
			tokenNames = new HashMap<>();
			for (AssignedToken tkn : tokenList) {
				tokenNames.put(tkn.name, new Color((int) (Math.random() * Integer.MAX_VALUE)));
			}
		}
		if (grammarNames == null) {
			grammarNames = new HashMap<>();
			for (String tkn : ref.grammarNames()) {
				grammarNames.put(tkn, new Color((int) (Math.random() * Integer.MAX_VALUE)));
			}
		}
		int h     = g.getFontMetrics().getHeight() + 6;
		int x_max = 0;
		y_off = 3;
		x_off = 3;

		drawHistoryMap(g, h, tokenNames);
		x_max = Math.max(x_max, x_off);
		y_off += 3;
		x_off = 3;
		drawHistoryMap(g, h, grammarNames);
		x_max = Math.max(x_max, x_off);
		x_off = 3;
		y_off += h;
		for (AssignedToken tkn : tokenList) {
			drawBox(g, h, Collections.singletonList(tkn), tokenNames.get(tkn.name));
		}
		x_max = Math.max(x_max, x_off);
		y_off += h;
		x_off = 3;
		for (AssignedToken tkn : res.join()) {
			drawBox(g, h, Collections.singletonList(tkn), tokenNames.get(tkn.name));
		}
		x_off = 3;
		y_off += h;
		drawTree(g, h, res);
		x_max = Math.max(x_max, x_off);
		this.setPreferredSize(new Dimension(x_max, y_off));
	}

	private void drawHistoryMap(Graphics2D g,
	                            int h,
	                            Map<String, Color> map) {
		for (Map.Entry<String, Color> tkn : map.entrySet()) {
			String val = tkn.getKey();
			int    w   = g.getFontMetrics().stringWidth(val) + 6;
			g.setColor(tkn.getValue());
			g.fillRect(x_off, y_off, w, h);
			g.setColor(Color.BLACK);
			g.drawRect(x_off, y_off, w, h);
			g.drawString(val, 3 + x_off, h - 3 + y_off);
			x_off += w;
		}
		y_off += h;
	}

	private void drawTree(Graphics2D g, int h, GrammarResult res) {

		if(res instanceof TokenGrammarResult && res.getOriginGrammarName() == null)
			return;
		if (res == null) {
			return;
		}
		int lx_off = x_off;
		Color bx = grammarNames.get(res.getOriginGrammarName());
		if(bx == null){
			System.out.println(res.getOriginGrammarName()+" has no color");bx = Color.WHITE;}
		drawBox(
				g,
				h,
				res.join(),
				bx
		);
		{
			int exchange = lx_off;
			lx_off = x_off;
			x_off  = exchange;
		}
		y_off += h;
		if (res instanceof SuperGrammarResult) {
			for (GrammarResult subRes : ((SuperGrammarResult) res).objects) {
				drawTree(g, h, subRes);
			}
		}

		if (res instanceof ListGrammarResult) {
			for (GrammarResult subRes : ((ListGrammarResult) res).getList()) {
				drawTree(g, h, subRes);
			}
		}

		if (res instanceof OptionalGrammarResult) {
			drawTree(g,h,((OptionalGrammarResult) res).getValue());
		}
		if (res instanceof AnyGrammarResult) {
			drawTree(g,h,((AnyGrammarResult) res).getRes());
		}
		y_off -= h;
		x_off = lx_off;
	}

	private void drawBox(Graphics2D g,
	                     int h,
	                     Collection<? extends AssignedToken> text,
	                     Color color) {
		int w = text
		                  .stream()
		                  .mapToInt(e -> g.getFontMetrics().stringWidth(e.value))
		                  .sum() + (3 * text.size());
		g.setColor(color);
		g.fillRect(x_off, y_off, w, h);
		g.setColor(Color.BLACK);
		g.drawRect(x_off, y_off, w, h);
		for (AssignedToken c : text) {
			String val = c.value;
			val = val.replaceAll("\n", "\\\\n");
			val = val.replaceAll("\r", "\\\\r");
			x_off += 3;
			g.drawString(val, x_off, h - 1 + y_off);
			x_off += g.getFontMetrics().stringWidth(val);
		}
	}

	public void display() {
		JFrame      jf = new JFrame();
		JScrollPane sp = new JScrollPane();
		sp.setViewportView(this);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jf.getContentPane().add(sp);
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		jf.setVisible(true);
	}
}

package com.niton;

import com.niton.parser.GrammarResult;
import com.niton.parser.token.Tokenizer.AssignedToken;
import com.niton.parser.result.*;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


public class ResultDisplay extends com.niton.media.visual.Canvas {
    private SuperGrammarResult res;
    @Getter
    @Setter
    private List<AssignedToken> tokenList;
    private Font f = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static HashMap<Class<? extends GrammarResult>,Color> resColorMap = new HashMap();
    static {
        resColorMap.put(AnyGrammarResult.class, Color.CYAN);
        resColorMap.put(IgnoredGrammarResult.class, Color.red);
        resColorMap.put(TokenGrammarResult.class, Color.GREEN);
        resColorMap.put(SuperGrammarResult.class, Color.PINK);
    }

    public ResultDisplay(SuperGrammarResult res) {
        this.res = res;
        tokenList = res.join();
        setFps(1);
    }

    private Map<String, Color> tokenNames;

    @Override
    public void paint(Graphics2D g, int i) {
        g.setFont(f);
        int x_off = 0;
        int y_off = 0;
        if (tokenNames == null) {
            tokenNames = new HashMap<>();
            for (AssignedToken tkn : tokenList) {
                tokenNames.put(tkn.name, new Color((int) (Math.random() * Integer.MAX_VALUE)));
            }
        }

        y_off = 3;
        x_off = 3;
        int h = g.getFontMetrics().getHeight() + 6;
        for (Map.Entry<String, Color> tkn : tokenNames.entrySet()) {
            String val = tkn.getKey();
            int w = g.getFontMetrics().stringWidth(val) + 6;
            g.setColor(tkn.getValue());
            g.fillRect(x_off, y_off, w, h);
            g.setColor(Color.BLACK);
            g.drawRect(x_off, y_off, w, h);
            g.drawString(val, 3 + x_off, h - 3 + y_off);
            x_off += w;
        }
        y_off += h;
        x_off = 0;
        y_off += 3;

        for (AssignedToken tkn : tokenList) {
            x_off = drawBox(g, x_off, y_off, h, tkn.value, tokenNames.get(tkn.name));
        }
        y_off += h;
        x_off = 0;
        this.setPreferredSize(new Dimension(x_off + 6, y_off));
        for (AssignedToken tkn : res.join()) {
            x_off = drawBox(g, x_off, y_off, h, tkn.value, tokenNames.get(tkn.name));
        }
        x_off = 0;
        y_off += h;
        drawTree(g, x_off, y_off, h, res);
    }

    private int drawTree(Graphics2D g, int x_off, int y_off, int h, GrammarResult res) {
        int absW = 0;
        int lx_off = x_off;
        absW = res.join().stream().mapToInt(e->g.getFontMetrics().stringWidth(e.value)).sum() + (6 * res.join().size());

        lx_off = drawBox(g, x_off, y_off, h, res.getOriginGrammarName() != null ? res.getOriginGrammarName() : new String(), resColorMap.get(res.getClass()), absW);
        y_off += h;
        lx_off = x_off;

//        if (res instanceof TokenGrammarResult || res instanceof IgnoredGrammarResult) {
//            for (AssignedToken tkn : res.join()) {
//                lx_off = drawBox(g, lx_off, y_off, h, tkn.value, tokenNames.get(tkn.name));
//            }
//            y_off += h;
//        }

        if (res instanceof SuperGrammarResult) {
            for (GrammarResult subRes : ((SuperGrammarResult) res).objects)
                lx_off = drawTree(g, lx_off, y_off, h, subRes);
            lx_off = x_off;
            y_off += h;
        }

        if (res instanceof ListGrammarResult) {
            for (GrammarResult subRes : ((ListGrammarResult) res).getList())
                lx_off = drawTree(g, lx_off, y_off, h, subRes);
            lx_off = x_off;
            y_off += h;
        }
        if(res instanceof AnyGrammarResult){
            drawTree(g, lx_off, y_off, h, ((AnyGrammarResult) res).getRes());
            y_off += h;
        }
        if(res instanceof OptionalGrammarResult) {
            drawTree(g, lx_off, y_off, h, ((OptionalGrammarResult) res).getValue());
            y_off += h;
        }
        return lx_off;
    }

    private int drawBox(Graphics2D g, int x_off, int y_off, int h, String text, Color color, int absW) {
        int w = absW;
        g.setColor(color);
        g.fillRect(x_off, y_off, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x_off, y_off, w, h);
        g.drawString(text, 3 + x_off, h - 3 + y_off);
        x_off += w;
        return x_off;
    }

    private int drawBox(Graphics2D g, int x_off, int y_off, int h, String text, Color c) {
        String val = text;
        val = val.replaceAll("\n", "\\\\n");
        val = val.replaceAll("\r", "\\\\r");
        return drawBox(g, x_off, y_off, h, val, c, g.getFontMetrics().stringWidth(val) + 6);
    }

    public void display() {
        JFrame jf = new JFrame();
        jf.setMinimumSize(new Dimension(900, 700));
        JScrollPane sp = new JScrollPane();
        sp.setViewportView(this);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jf.getContentPane().add(sp);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setVisible(true);
    }
}

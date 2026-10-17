package dev.sigmarix.quests;

/** Screen-space bounds shared by painting, clipping and mouse hit tests. */
public final class QuestViewLayout {
    public final int x,y,width,height,columns,gridRows,textTop,bodyBottom;
    public QuestViewLayout(int screenWidth,int screenHeight,int titleWidth,int minWidth,int tasks,int rewards,int textLines,boolean guide) {
        width=Math.max(120,Math.min(screenWidth-2,Math.max(201,Math.max(titleWidth+50,minWidth))|1));
        columns=Math.max(1,(width/2-6)/20);
        gridRows=Math.max(1,(Math.max(tasks,rewards)+columns-1)/columns);
        textTop=16+16+gridRows*20-2+12;
        height=Math.min(Math.max(80,screenHeight-10),Math.max(textTop+8,textTop+textLines*9+(guide?22:0)+4));
        x=(screenWidth-width)/2; y=(screenHeight-height)/2;
        bodyBottom=y+height-3;
    }
    public boolean contains(int px,int py) { return px>=x && px<x+width && py>=y && py<y+height; }
    public int cellX(boolean reward,int index,int count) {
        int column=index%columns,row=index/columns,n=Math.min(columns,count-row*columns);
        return x+(reward?width/2:0)+(width/2-n*20)/2+column*20+1;
    }
    public int cellY(int index) { return y+32+index/columns*20; }
}

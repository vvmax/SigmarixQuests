package dev.sigmarix.quests;

/** Screen-space bounds shared by painting, clipping and mouse hit tests. */
public final class QuestViewLayout {
    public final int x,y,width,height,columns,gridRows,textTop,bodyBottom;
    private final QuestCompactGrid tasksGrid,rewardsGrid;
    private final int gridHeight;
    public QuestViewLayout(int screenWidth,int screenHeight,int titleWidth,int minWidth,int tasks,int rewards,int textLines,boolean guide) {
        this(screenWidth,screenHeight,titleWidth,minWidth,tasks,rewards,textLines,guide,textLines*9+(guide?13:0));
    }
    public QuestViewLayout(int screenWidth,int screenHeight,int titleWidth,int minWidth,int tasks,int rewards,int textLines,boolean guide,int textHeight) {
        width=Math.max(120,Math.min(screenWidth-2,Math.max(201,Math.max(titleWidth+50,minWidth))|1));
        columns=4;
        tasksGrid=new QuestCompactGrid(Math.max(1,tasks),20); rewardsGrid=new QuestCompactGrid(Math.max(1,rewards),20);
        gridHeight=Math.max(tasksGrid.alignedHeight,rewardsGrid.alignedHeight);
        gridRows=gridHeight/20;
        textTop=44+gridHeight;
        height=Math.min(Math.max(40,screenHeight-10),textLines==0 && !guide?gridHeight+40:textTop+textHeight+4);
        x=(screenWidth-width)/2; y=(screenHeight-height)/2;
        bodyBottom=y+height-3;
    }
    public boolean contains(int px,int py) { return px>=x && px<x+width && py>=y && py<y+height; }
    public int cellX(boolean reward,int index,int count) {
        QuestCompactGrid grid=reward?rewardsGrid:tasksGrid;
        return x+(reward?width/2:0)+2+(width/2-3-grid.width)/2+grid.x(index);
    }
    public int cellY(boolean reward,int index) {
        QuestCompactGrid grid=reward?rewardsGrid:tasksGrid;
        return y+32+(gridHeight-grid.height)/2+grid.y(index);
    }
    public int cellY(int index) { return cellY(false,index); }
}

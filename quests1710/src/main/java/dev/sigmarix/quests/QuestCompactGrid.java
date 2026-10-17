package dev.sigmarix.quests;
/** The compact row patterns used by the reference library, in logical GUI pixels. */
public final class QuestCompactGrid {
    private static final int[][] ROWS={{1},{2},{3},{4},{3,2},{3,3},{4,3},{4,4},{3,3,3},{3,4,3},{4,3,4},{4,4,4},{4,3,3,3},{3,4,4,3},{4,4,4,3},{4,4,4,4}};
    public final int size,count,width,height,alignedHeight;
    private final int[] xs,ys;
    public QuestCompactGrid(int count,int size) {
        this.count=Math.max(0,count);this.size=size;xs=new int[this.count];ys=new int[this.count];
        int maxX=0,maxY=0,aligned=0;
        if(this.count>16) { for(int i=0;i<this.count;i++){xs[i]=i%4*size;ys[i]=i/4*size;} maxX=4*size;maxY=((this.count+3)/4)*size;aligned=this.count/4*size; }
        else if(this.count>0) {
            int[] rows=ROWS[this.count-1];int columns=0,index=0;for(int n:rows)columns=Math.max(columns,n);
            for(int row=0;row<rows.length;row++)for(int column=0;column<rows[row];column++){xs[index]=((rows[row]%2==columns%2)?0:size/2)+column*size;ys[index]=row*size;maxX=Math.max(maxX,xs[index]+size);index++;}
            maxY=aligned=rows.length*size;
        }
        width=maxX;height=maxY;alignedHeight=aligned;
    }
    public int x(int index){return xs[index];}
    public int y(int index){return ys[index];}
}

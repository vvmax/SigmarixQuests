package dev.sigmarix.quests.client;
import com.google.gson.*;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.*;
/** Recursive typed object forms over a private draft. Nothing changes on the server until Save. */
public final class QuestObjectScreen extends GuiScreen {
    private static final class Session {
        JsonObject root; long revision; boolean pending; String message=QuestText.tr("Select a field to edit; nested lists open their own forms.");
        GuiScreen destination;
    }
    private final Session session;
    private final GuiScreen parent;
    private final JsonElement node;
    private final String field,path;
    private int scroll,selected=-1;
    private List<String> keys=new ArrayList<String>();
    public QuestObjectScreen(GuiScreen parent) {
        this.parent=parent; session=new Session(); session.root=QuestEditing.tree(ClientProxy.snapshot.book).getAsJsonObject(); session.revision=ClientProxy.snapshot.revision; session.destination=parent;
        node=session.root; field="book"; path="Book";
    }
    /** Opens a scoped draft while preserving the complete book for validation and atomic save. */
    public QuestObjectScreen(GuiScreen parent,String questId,String collection,String objectId) {
        this.parent=parent;session=new Session();session.root=QuestEditing.tree(ClientProxy.snapshot.book).getAsJsonObject();session.revision=ClientProxy.snapshot.revision;session.destination=parent;
        JsonElement selected=session.root;String scope="book",label="Book";
        if(!questId.isEmpty()) for(JsonElement entry:session.root.getAsJsonArray("quests")) if(entry.getAsJsonObject().get("id").getAsString().equals(questId)) {
            selected=entry;scope="quests";label=questId;
            if(!collection.isEmpty()) {selected=entry.getAsJsonObject().get(collection);scope=collection;label+=" / "+collection;
                if(objectId!=null && !objectId.isEmpty()) for(JsonElement value:selected.getAsJsonArray()) if(value.getAsJsonObject().has("id") && value.getAsJsonObject().get("id").getAsString().equals(objectId)){selected=value;break;}
            }
            break;
        }
        if(questId.isEmpty() && !collection.isEmpty()) {selected=session.root.get(collection);scope=collection;label=collection;if(objectId!=null && !objectId.isEmpty() && selected.isJsonObject() && selected.getAsJsonObject().has(objectId)){selected=selected.getAsJsonObject().get(objectId);scope=collection.equals("lootTables")?"lootEntries":collection;label+=" / "+objectId;}}
        node=selected;field=scope;path=label;
    }
    private QuestObjectScreen(QuestObjectScreen parent,JsonElement node,String field,String label) { this.parent=parent; session=parent.session; this.node=node; this.field=field; path=parent.path+" / "+label; }
    @Override public void initGui() { rebuild(); }
    private int size() { return node.isJsonArray()?node.getAsJsonArray().size():keys.size(); }
    private JsonElement get(int index) { return node.isJsonArray()?node.getAsJsonArray().get(index):node.getAsJsonObject().get(keys.get(index)); }
    private String key(int index) { return node.isJsonArray()?Integer.toString(index):keys.get(index); }
    private boolean collection() { return node.isJsonArray() || field.equals("lootTables") || field.equals("lootSettings"); }
    private void rebuild() {
        if(node.isJsonObject()) keys=QuestEditing.keys(node.getAsJsonObject());
        buttonList.clear(); int rows=Math.max(1,(height-32)/12); scroll=Math.max(0,Math.min(scroll,Math.max(0,size()-rows)));
        String[] icons={"accept","cancel","settings","add","remove","notes","inv_in","up","down"};
        String[] actions={"Save book","Back","Raw JSON","Add","Delete","Copy","Paste","Up","Down"};
        for(int i=0;i<actions.length;i++) {
            QuestFlatButton button=new QuestFlatButton(i,width-18-i*20,2,16,16,QuestText.tr(actions[i])).glyph(icons[i],true);
            buttonList.add(button);
        }
        for(int i=scroll;i<size() && i<scroll+rows;i++) {
            JsonElement value=get(i);
            buttonList.add(new ConfigEntry(100+i,0,20+(i-scroll)*12,width-16,key(i),summary(value),value));
        }
        for(Object raw:buttonList) { GuiButton b=(GuiButton)raw;
            if(b.id==0) b.enabled=!session.pending;
            if(b.id==3 || b.id==6) b.enabled=collection() && !session.pending;
            if(b.id==4) b.enabled=(collection() || node.isJsonObject() && selected>=0 && selected<size() && key(selected).equals("filter")) && selected>=0 && selected<size() && !session.pending;
            if(b.id==5) b.enabled=selected>=0 && selected<size();
            if(b.id==7 || b.id==8) b.enabled=node.isJsonArray() && selected>=0 && selected<size() && !session.pending;
        }
    }
    private String summary(JsonElement e) {
        if(e.isJsonArray()) return "["+e.getAsJsonArray().size()+" entries]";
        if(e.isJsonObject()) { JsonObject o=e.getAsJsonObject(); return o.has("title") && o.get("title").isJsonPrimitive()?o.get("title").getAsString():o.has("type") && o.get("type").isJsonPrimitive()?o.get("type").getAsString():"{fields}"; }
        return e.isJsonNull()?"(empty)":e.getAsString().replace("\n"," ");
    }
    private void set(int index,JsonElement value) { if(node.isJsonArray()) QuestEditing.set(node.getAsJsonArray(),index,value); else node.getAsJsonObject().add(keys.get(index),value); }
    @Override protected void actionPerformed(GuiButton b) {
        if(b.id==1) { mc.displayGuiScreen(parent); return; }
        if(b.id==5) { setClipboardString(get(selected).toString()); session.message=QuestText.tr("Copied selected object"); return; }
        if(session.pending) return;
        try {
            if(b.id>=100) {
                selected=b.id-100; JsonElement value=get(selected); String key=key(selected);
                if(value.isJsonNull() && key.equals("filter")) { value=QuestEditing.tree(new QuestItemFilter()); set(selected,value); }
                final JsonElement fieldValue=value;
                if(value.isJsonArray() || value.isJsonObject()) {
                    String context=field.equals("lootTables")?"lootEntries":key;
                    mc.displayGuiScreen(new QuestObjectScreen(this,value,context,key));
                } else if(fieldValue.isJsonPrimitive() && fieldValue.getAsJsonPrimitive().isBoolean()) set(selected,new JsonPrimitive(!value.getAsBoolean()));
                else { final int index=selected; mc.displayGuiScreen(new ValueScreen(this,key,value,text->{
                    JsonElement next=fieldValue.isJsonPrimitive() && fieldValue.getAsJsonPrimitive().isNumber()?new JsonParser().parse(text):new JsonPrimitive(text);
                    if(fieldValue.isJsonPrimitive() && fieldValue.getAsJsonPrimitive().isNumber() && (!next.isJsonPrimitive() || !next.getAsJsonPrimitive().isNumber())) throw new IllegalArgumentException("Expected a number");
                    set(index,next);
                })); }
            } else if(b.id==0) {
                QuestCore.Book draft=QuestService.JSON.fromJson(session.root,QuestCore.Book.class); draft.validate();
                QuestNetwork.Action action=new QuestNetwork.Action(); action.action="save"; action.revision=session.revision; action.book=draft;
                ClientProxy.send(action); session.pending=true; session.message=QuestText.tr("Saving...");
            } else if(b.id==2) mc.displayGuiScreen(new QuestEditorScreen(session.destination,QuestService.JSON.fromJson(session.root,QuestCore.Book.class),session.revision));
            else if(b.id==3 || b.id==6) {
                if(node.isJsonArray()) {
                    JsonElement value=b.id==3?QuestEditing.template(field):QuestEditing.duplicate(new JsonParser().parse(getClipboardString()));
                    // Prevent arrays/objects being pasted into primitive-only fields or vice versa.
                    boolean object=QuestEditing.template(field).isJsonObject(); if(object!=value.isJsonObject()) throw new IllegalArgumentException("Clipboard object does not match list type");
                    node.getAsJsonArray().add(value); selected=node.getAsJsonArray().size()-1; scroll=selected;
                } else { final boolean paste=b.id==6; mc.displayGuiScreen(new ValueScreen(this,"New table ID",new JsonPrimitive("new_table"),text->{
                    if(!QuestCore.validId(text) || node.getAsJsonObject().has(text)) throw new IllegalArgumentException("Invalid or existing table ID");
                    JsonElement value=paste?QuestEditing.copy(new JsonParser().parse(getClipboardString())):field.equals("lootTables")?new JsonArray():QuestService.JSON.toJsonTree(new QuestCore.LootSettings());
                    if(field.equals("lootTables")?!value.isJsonArray():!value.isJsonObject()) throw new IllegalArgumentException("Wrong table value type");
                    node.getAsJsonObject().add(text,value);
                })); }
            } else if(b.id==4) { if(node.isJsonArray()) QuestEditing.remove(node.getAsJsonArray(),selected); else if(key(selected).equals("filter") && !collection()) node.getAsJsonObject().add("filter",JsonNull.INSTANCE); else node.getAsJsonObject().remove(keys.get(selected)); selected=-1; }
            else if(b.id==7 || b.id==8) { int delta=b.id==7?-1:1; QuestEditing.move(node.getAsJsonArray(),selected,delta); selected=Math.max(0,Math.min(size()-1,selected+delta)); }
        } catch(RuntimeException ex) { session.message="Validation: "+ex.getMessage(); }
        rebuild();
    }
    public void saved(QuestNetwork.Snapshot snapshot) {
        if(!session.pending) return; session.pending=false; session.message=snapshot.message;
        if(snapshot.revision!=session.revision && !snapshot.message.startsWith("Error:")) mc.displayGuiScreen(session.destination);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground();drawRect(0,0,width,20,0x99333333);
        fontRendererObj.drawStringWithShadow("\u00a7l"+fontRendererObj.trimStringToWidth(path,Math.max(30,width-190)),6,6,0xFFFFFF);
        drawRect(width-16,20,width,height,0x46000000);
        int rows=Math.max(1,(height-32)/12),track=height-20,thumb=Math.max(16,size()<=rows?track:track*rows/size());
        int sy=20+(size()<=rows?0:scroll*(track-thumb)/(size()-rows));
        drawRect(width-15,sy+1,width-1,sy+thumb-1,0x64FFFFFF);QuestClassicUi.frame(width-15,sy+1,14,thumb-2,0x64808080);
        super.drawScreen(x,y,partial);
        for(Object raw:buttonList)if(raw instanceof QuestFlatButton){QuestFlatButton button=(QuestFlatButton)raw;if(x>=button.xPosition && x<button.xPosition+16 && y>=2 && y<18)drawHoveringText(Collections.singletonList(button.tooltip),x,y,fontRendererObj);}
        if(session.message.startsWith("Validation:"))drawCenteredString(fontRendererObj,session.message,width/2,height-12,0xFFFF7777);
    }
    private static final class ConfigEntry extends GuiButton {
        final String key,value;final JsonElement element;
        ConfigEntry(int id,int x,int y,int width,String key,String value,JsonElement element){super(id,x,y,width,12,key);this.key=key;this.value=value;this.element=element;}
        @Override public void drawButton(net.minecraft.client.Minecraft mc,int x,int y){
            field_146123_n=x>=xPosition && x<xPosition+width && y>=yPosition && y<yPosition+height;
            if(field_146123_n)drawRect(xPosition,yPosition,xPosition+width,yPosition+height,0x21FFFFFF);
            String text=mc.fontRenderer.trimStringToWidth(value,150);if(!text.equals(value))text+="...";
            int vw=mc.fontRenderer.getStringWidth(text),vx=xPosition+width-vw-4;
            int color=element.isJsonPrimitive()?(element.getAsJsonPrimitive().isBoolean()?(element.getAsBoolean()?0x33AA33:0xD52834):element.getAsJsonPrimitive().isNumber()?0xAA5AE8:0xFFAA49):0xAAAAAA;
            mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(key,Math.max(0,vx-xPosition-12)),xPosition+4,yPosition+2,0xFFFFFF,field_146123_n);
            mc.fontRenderer.drawString(text,vx,yPosition+2,color);
        }
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); scroll+=wheel>0?-2:wheel<0?2:0; rebuild(); }
    @Override protected void keyTyped(char ch,int key) {
        if(key==Keyboard.KEY_DELETE && collection() && selected>=0) { actionPerformed(new QuestFlatButton(4,0,0,"")); return; }
        super.keyTyped(ch,key);
    }
    @Override public boolean doesGuiPauseGame() { return false; }
    private static final class ValueScreen extends GuiScreen {
        private final QuestObjectScreen parent; private final String label; private final JsonElement initial; private final java.util.function.Consumer<String> apply;
        private GuiTextField input; private int left,top; private String message="Use \\n for a line break. Tab completes common field values.";
        ValueScreen(QuestObjectScreen parent,String label,JsonElement initial,java.util.function.Consumer<String> apply) { this.parent=parent; this.label=label; this.initial=initial; this.apply=apply; }
        @Override public void initGui() {
            Keyboard.enableRepeatEvents(true);left=(width-230)/2;top=(height-54)/2;
            input=new QuestLibraryTextField(fontRendererObj,left+12,top+12,206,12);input.setEnableBackgroundDrawing(false);input.setMaxStringLength(32768);input.setText(initial.isJsonNull()?"":initial.getAsString().replace("\\","\\\\").replace("\n","\\n"));input.setFocused(true);input.setTextColor(initial.isJsonPrimitive() && initial.getAsJsonPrimitive().isNumber()?0xAA5AE8:0xFFAA49);
            buttonList.clear();buttonList.add(new QuestLibraryUi.Button(1,left+8,top+30,105,16,QuestText.tr("Cancel"),""));buttonList.add(new QuestLibraryUi.Button(0,left+117,top+30,105,16,QuestText.tr("Accept"),""));
        }
        @Override protected void actionPerformed(GuiButton b) {
            if(b.id==1) { mc.displayGuiScreen(parent); return; }
            if(b.id==2) { cycle(); return; }
            try { String raw=input.getText(); StringBuilder text=new StringBuilder();
                for(int i=0;i<raw.length();i++) { char c=raw.charAt(i); if(c=='\\' && i+1<raw.length()) { char n=raw.charAt(++i); if(n=='n') text.append('\n'); else { if(n!='\\') text.append('\\'); text.append(n); } } else text.append(c); }
                apply.accept(text.toString()); mc.displayGuiScreen(parent);
            } catch(RuntimeException ex) { message=ex.getMessage(); }
        }
        private void cycle() {
            if(!Arrays.asList("type","shape","mode","rewardMode","dependencyState","nbtMode","auto","matchType","comparison","defaultQuestShape","itemBrowser").contains(label)) return;
            List<String> values;
            if(label.equals("type") && parent.node.isJsonObject() && parent.node.getAsJsonObject().has("resource")) values=Arrays.asList("text","image","item","link");
            else if(label.equals("matchType")) { values=new ArrayList<String>(QuestRayMatcher.TYPES); Collections.sort(values); }
            else if(label.equals("comparison")) values=Arrays.asList("at_least","at_most","equals","not_equals","greater","less");
            else if(label.equals("type")) { values=new ArrayList<String>(parent.node.isJsonObject() && parent.node.getAsJsonObject().has("children")?QuestItemFilter.TYPES:parent.field.equals("tasks") || parent.path.contains(" / tasks / ")?QuestCore.TASK_TYPES:QuestCore.REWARD_TYPES); Collections.sort(values); }
            else if(label.equals("itemBrowser")) values=Arrays.asList("inherit","enabled","disabled");
            else if(label.equals("shape") || label.equals("defaultQuestShape")) { values=new ArrayList<String>(QuestPresentation.SHAPES); if(!parent.field.equals("book")) values.add(""); Collections.sort(values); }
            else if(label.equals("mode")) values=Arrays.asList("inherit","team","individual");
            else if(label.equals("rewardMode")) values=Arrays.asList("team","individual");
            else if(label.equals("auto")) values=Arrays.asList("inherit","enabled","disabled","invisible");
            else if(label.equals("dependencyState")) values=Arrays.asList("completed","started");
            else values=Arrays.asList("exact","subset","ignore");
            input.setText(values.get((values.indexOf(input.getText())+1)%values.size()));
        }
        @Override protected void keyTyped(char ch,int key) { if(key==Keyboard.KEY_RETURN) actionPerformed(new QuestFlatButton(0,0,0,"")); else if(key==Keyboard.KEY_TAB) cycle(); else if(key==Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); else input.textboxKeyTyped(ch,key); }
        @Override protected void mouseClicked(int x,int y,int b) { super.mouseClicked(x,y,b); input.mouseClicked(x,y,b); }
        @Override public void updateScreen() { input.updateCursorCounter(); }
        @Override public void drawScreen(int x,int y,float partial){drawDefaultBackground();QuestLibraryUi.window(left,top,230,54);QuestLibraryUi.textBox(left+8,top+8,214,16);input.drawTextBox();if(!message.startsWith("Use "))drawCenteredString(fontRendererObj,message,width/2,top-12,0xFFFF7777);super.drawScreen(x,y,partial);}
        @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
        @Override public boolean doesGuiPauseGame() { return false; }
    }
}

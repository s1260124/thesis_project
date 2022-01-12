package application;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
	@Override
	public void start(Stage stage) {
		stage.setTitle("C program compile error checker");
		Group root = new Group();
		Scene scene = new Scene(root,1050,800);
		stage.setScene(scene);
		//stage.setMaximized(true);//全画面表示
		scene.getStylesheets().add(this.getClass().getResource("application.css").toExternalForm());//CSSファイルを読み込む

		BorderPane borderPane = new BorderPane();
		root.getChildren().add(borderPane);
		
		stage.show();

/*----------borderPane:TOP---------------*/
/*コンパイルボタン配置*/
		HBox menu = new HBox();
		borderPane.setTop(menu);

		Button compileButton = new Button("Compile");
		menu.setPadding(new Insets(10, 10, 0, 10));//(top/right/bottom/left)
		menu.getChildren().add(compileButton);

/*----------borderPane:LEFT---------------*/
/*ソースコード編集エリア配置*/
		HBox left = new HBox();
		left.setPadding(new Insets(0, 10, 0, 10));//(top/right/bottom/left)
		left.prefWidthProperty().bind(stage.widthProperty().multiply(0.3));
		left.prefHeightProperty().bind(stage.heightProperty().multiply(0.85));

		TextArea sourceCodeArea = new TextArea();
		sourceCodeArea.setPrefHeight(stage.getHeight()*0.85);
		left.getChildren().add(sourceCodeArea);

		borderPane.setLeft(left);

/*----------borderPane:CENTER---------------*/
/*ソースコードエラー箇所指摘エリア配置*/
		HBox center = new HBox();

		Label LineNumber = new Label();
		LineNumber.setId("linenumber");//css用
		LineNumber.setVisible(false);

		Pane compiledCode = new Pane();
		compiledCode.setVisible(false);

		center.getChildren().addAll(LineNumber,compiledCode);

		ScrollPane sp1 = new ScrollPane();
		sp1.prefHeightProperty().bind(stage.heightProperty().multiply(0.85));
		sp1.prefWidthProperty().bind(stage.widthProperty().multiply(0.3));
		sp1.setContent(center);
		sp1.setHbarPolicy(ScrollBarPolicy.NEVER);

		borderPane.setCenter(sp1);
		BorderPane.setAlignment(sp1, Pos.TOP_LEFT);

/*----------borderPane:RIGHT---------------*/
/*エラーメッセージ説明エリア配置*/
		VBox err_Label = new VBox(10);
		
		ScrollPane sp2 = new ScrollPane();
		sp2.prefHeightProperty().bind(stage.heightProperty().multiply(0.85));
		sp2.prefWidthProperty().bind(stage.widthProperty().multiply(0.39));
		sp2.setContent(err_Label);
		sp2.setHbarPolicy(ScrollBarPolicy.NEVER);
		sp2.setPadding(new Insets(0, 10, 0, 10));//(top/right/bottom/left)

		borderPane.setRight(sp2);
		BorderPane.setAlignment(sp2, Pos.TOP_RIGHT);

/*----------borderPane:BOTTOM---------------*/
/*エラーと警告の個数を表示（検出できないものも含む）*/
		Pane kosuu = new Pane();
		kosuu.setPadding(new Insets(10, 10, 10, 10));//(top/right/bottom/left)
		borderPane.setBottom(kosuu);

/*--------------------------------------------------------------------------------*/
		compileButton.setOnAction( e -> {
			try {
				LineNumber.setText("");//繰り返しの防止

				compileButton.setVisible(true);
				compileButton.setManaged(true);

				sourceCodeArea.setVisible(true);
				sourceCodeArea.setManaged(true);

				LineNumber.setVisible(true);
				compiledCode.setVisible(true);

				err_Label.getChildren().clear();
				compiledCode.getChildren().clear();

				kosuu.getChildren().clear();

				String currentDirectory = new File(".").getAbsoluteFile().getParent();
				FileOutputStream source_code = new FileOutputStream(currentDirectory + "/main.c");
				String code = sourceCodeArea.getText();
				source_code.write(code.getBytes());
				source_code.flush();
				source_code.close();
				String[] code1 = code.split("\n");
				for(int i=1;i<=code1.length;i++) {
					LineNumber.setText(LineNumber.getText() + i + " : \n");
				}

				/*コンパイル処理*/
				Process p = Runtime.getRuntime().exec("gcc main.c");
				new Stream(p.getInputStream(),"stdout.txt").start();
				new Stream(p.getErrorStream(),"stderr.txt").start();
				p.waitFor();
				p.destroy();

				/*メッセージ処理*/
				try {
					File errFile = new File(currentDirectory + "/stderr.txt");
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(errFile),"UTF-8"));
					boolean flag = true;//エラー・警告があるかどうか
					boolean detect = true;//検出できないエラー・警告があるかどうか
					String message;
					ArrayList<Label> error_message = new ArrayList<Label>();
					ArrayList<Integer> error_line_num = new ArrayList<Integer>();//errの場合
					ArrayList<Integer> warning_line_num = new ArrayList<Integer>();//warningの場合

					while((message = br.readLine()) != null) {
						Label err = new Label();//「○行目」用
						Label err2 = new Label();//日本語でのエラー内容とその解決策提案用
						err2.setStyle("-fx-wrap-text: true;");
						err2.setMaxWidth(sp2.getWidth()*0.89);
						err.setMaxHeight(stage.getHeight());

						HBox pa = new HBox();

						Label count = new Label();
						kosuu.getChildren().add(count);//エラーと警告の個数を表示
						String warning_number = "0";//初期値…エラー０個、警告０個
						String err_number = "0";

						if(message.contains("use of undeclared identifier") || message.contains("undeclared (first use in this function)")){
							flag = false;
							int index1 = message.indexOf("'");
							int index2 = message.indexOf("'",index1+1);
							String v_name = message.substring(index1+1, index2);
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText(v_name + "が未定義\n '[データ型] " + v_name + ";'を追加してください。\n "
									+ "定義した変数のみ使用することができます。\n "
									+ "そうでなければ、" + v_name + "はスペルミスの可能性があります。"
											+ "今一度確認してください。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("too few arguments to function")) {
							flag = false;
							String fun_name = (br.readLine()).trim();
							fun_name = fun_name.substring(0, fun_name.indexOf(";"));
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText(fun_name + "へ与える引数が足りない\n" +
									fun_name + "へと与える引数を今一度確認してください。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("too many arguments to function")) {
							flag = false;
							String fun_name = (br.readLine()).trim();
							fun_name = fun_name.substring(0, fun_name.indexOf(";"));
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText(fun_name + "へ与える引数が多い\n" +
									fun_name + "へと与える引数を今一度確認してください。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("No such file or directory")) {
							flag = false;
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText("ディレクトリ・ファイルが存在しない\n" +
									"ディレクトリ・ファイル名が正しいか確認をしてください。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("invalid preprocessing directive")) {
							flag = false;
							String pre_dir = (br.readLine()).trim();
							pre_dir = pre_dir.substring(0, pre_dir.indexOf("<"));//あとで調整
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText(pre_dir + "が無効\n" +
									pre_dir +  "は正しい命令ですか？タイプミスの可能性があります。"
											+ "確認を行ってください。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("redefinition of") || message.contains("redeclaration of")) {
							flag = false;
							String v_name = (br.readLine()).trim();//add
							if(v_name.contains("=")) {
								v_name = v_name.substring(v_name.indexOf(" ")+1, v_name.indexOf("="));
							}else{
								v_name = v_name.substring(v_name.indexOf(" ")+1, v_name.indexOf(";"));
							}
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							if(message.contains("with a different type:")) {
								err.setId("err");
								err2.setText(v_name + "は異なるデータ型で複数定義\n"
										+ "'[データ型] " + v_name + ";'が複数回書かれています。\n"
										+ "同じ名前の変数は一つのみ定義することができます。\n"
										+ "どちらかの" + v_name + "を変更してください。\n");
							}
							else{
								err2.setText(v_name + "が複数定義\n" +
									"'[データ型] " + v_name + ";'が複数回書かれています。\n" +
									"同じ名前の変数は一つのみ定義することができます。\n" +
									"どちらかの" + v_name + "を変更してください。\n");
							}
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("syntax error before")) {
							flag = false;
							int index1 = message.indexOf("'");
							int index2 = message.indexOf("'",index1+1);
							String any = message.substring(index1+1, index2);
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText(any + "に文法エラー\n" + any + "の直前に文法エラーが存在します。\n" +
									"『;』は抜けていませんか？『(』、『)』は多くありませんか？\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("expected ';'") || message.contains("expected '}'")) {
							flag = false;
							String any2 = (br.readLine()).trim();//add
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText(";または}が期待されている\n " + any2 + "の直後に文法エラーが存在します。\n" +
						"『;』は抜けていませんか？"
						+ "『{』、『}』は多かったり少なかったりしていませんか？\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("file not found")) {
							flag = false;
							String h_file = (br.readLine()).trim();//add
							h_file = h_file.substring(h_file.indexOf("<")+1, h_file.indexOf(">"));
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText(h_file + "が見つからない\n" +
									"このヘッダーファイルの名前を見直してください。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("expected \"FILENAME\" or <FILENAME>")) {
							flag = false;
							String h_file = (br.readLine()).trim();//add
							h_file = h_file.substring(h_file.indexOf("(")+1, h_file.indexOf(")"));
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText("ヘッダーファイルの記述に不備\n\n"
									+ "ヘッダーファイルは＜" + h_file + "＞または\" " + h_file + "\"と記述してください。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						else if(message.contains("subscripted value is not an array, pointer, or vector")) {
							flag = false;
							String index_name = (br.readLine()).trim();//add
							String[] a = index_name.split("[\\s]*,[\\s]*");
							for(int i=0; i<a.length; i++){
							    String[] b = a[i].split("\\s");
							    for(int j=0; j<b.length; j++){
							        if(b[j].contains("[")) index_name = b[j];
							      }
							  }
							index_name = index_name.substring(0, index_name.indexOf("["));
							index_name = index_name.replace("=", "");
							index_name = index_name.replace(")", "");
							index_name = index_name.replace("(", "");

							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("err");
							err2.setText("添え字付きの値は、配列、ポインター、またはベクトルではない\n"
									+ "配列" + index_name + "の次元は定義時の次元と異なっている可能性があります。\n"
									+ "または、" + index_name + "は配列でない可能性があります。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							error_line_num.add(err_num);
						}
						/*-----warning--------------------------------------------------------------------------------*/
						else if(message.contains("implicit declaration of function")) {
							flag = false;
							int index1 = message.indexOf("'");
							int index2 = message.indexOf("'",index1+1);
							String fun_name = message.substring(index1+1, index2);
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("warning");
							err2.setText(fun_name + "が未宣言\n" +
									fun_name + "の定義にミス or タイプミスの可能性があります。\n" +
									"関数定義は\n" +
									"データ型 関数名(データ型 引数, …){\n" +
									"	命令\n" +
									"}\n" +
									"という形である必要があります。\n" +
									"そうでなければ定義した関数名と" + fun_name + "は一致していますか？\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							warning_line_num.add(err_num);
						}
						else if(message.contains("missing terminating '\"' character")){
							flag = false;
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("warning");
							err2.setText("終了文字『\"』がない\n"
									+ "『\"』をつけ忘れていませんか？\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							warning_line_num.add(err_num);
						}
						else if(message.contains("expression result unused")) {
							flag = false;
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("warning");
							err2.setText("式の結果が未使用\n"
									+ "式が間違っていて使われない可能性があります。\n"
									+ "例えば、for文やwhile文であれば、()内の条件文の記述ミスが考えられます。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							warning_line_num.add(err_num);
						}
						else if(message.contains("data argument not used by format string")) {
							flag = false;
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							err.setText(err_num + "\n行目");
							err.setId("warning");
							err2.setText("フォーマット文に引用されていない引数のデータがある\n"
									+ "printf文のフォーマット文の書式が間違っている可能性があります。\n"
									+ "また、引数漏れがあります。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							warning_line_num.add(err_num);
						}
						else if(message.contains("format specifies type")) {
							flag = false;
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);
							String v_type = (br.readLine()).trim();

							err.setText(err_num + "\n行目");
							err.setId("warning");
							err2.setText("形式上で指定している" + v_type + "と引数の型が異なる\n"
									+ "scanf文の場合、scanf(“%d”,&引数);です。"
									+ "この＆が抜けている可能性があります。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							warning_line_num.add(err_num);
						}
						else if(message.contains("array index") && message.contains("is past the end of the array")) {
							flag = false;
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);

							String index1 = message.substring(message.indexOf("index"), message.indexOf("is"));
							index1 = index1.replaceAll("[^0-9]", "");

							String index2 = message.substring(message.indexOf("contains"), message.indexOf("elements"));
							index2 = index2.replaceAll("[^0-9]", "");

							String index_name = (br.readLine()).trim();
							String[] a = index_name.split("[\\s]*,[\\s]*");
							for(int i=0; i<a.length; i++){
							    String[] b = a[i].split("\\s");
							    for(int j=0; j<b.length; j++){
							        if(b[j].contains("[")) index_name = b[j];
							      }
							  }
							index_name = index_name.substring(0, index_name.indexOf("["));
							index_name = index_name.replace("=", "");
							index_name = index_name.replace(")", "");
							index_name = index_name.replace("(", "");

							err.setText(err_num + "\n行目");
							err.setId("warning");
							err2.setText("配列の添え字" + index1 + "はその配列の末尾を過ぎている\n"
									+"配列" + index_name +"では、アクセスできるのは[0]~["
									+ (Integer.parseInt(index2)-1) + "]の" + index2 + "個の要素です。\n"
									+ "[" + index1 + "] は配列の領域外となり参照できません。\n");
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							warning_line_num.add(err_num);
						}
						else if(message.contains("treating Unicode character")) {
							flag = false;
							String[] strs = message.split(":");
							int err_num = Integer.parseInt(strs[1]);
							
							err.setText(err_num + "\n行目");
							err.setId("warning");
							if(message.contains("whitespace")) {
								err2.setText("全角文字が含まれている\n"
									+"プログラムは原則として半角英数記号です。\n"
									+ "スペース部分を確認し、半角に修正してみてください。\n");
							}
							else if(message.contains("identifier character rather than as")) {
								err2.setText("全角文字が含まれている\n"
										+"プログラムは原則として半角英数記号です。\n"
										+ "半角に修正してみてください。\n");
							}
							pa.setStyle("-fx-border-color: black; ");
							pa.getChildren().addAll(err,err2);
							error_message.add(err);
							err_Label.getChildren().add(pa);
							warning_line_num.add(err_num);
						}
						else if(message.contains("generated.")) {
							if(message.contains("warning")) warning_number = message.substring(0, message.indexOf("warning"));
							if(message.contains("error")) err_number = message.substring(message.indexOf("error")-2, message.indexOf("error"));

								count.setText("警告: " + warning_number + "個	\tエラー: " + err_number + "個\n");
								count.setPadding(new Insets(10, 10, 10, 10));//(top/right/bottom/left)
								count.setStyle("-fx-font-size: 17px;");
						}
						else if(message.contains("error") || message.contains("warning") ) {
							detect = false;
						}

					}
					if(detect == false){
						Label x = new Label();
						error_message.add(x);
						x.setText("本ツールで確認できないエラーがまだあります。\n"
								+ "近くのTAさんに質問しましょう。\n");
						x.setStyle("-fx-border-color: black;");
						err_Label.getChildren().add(x);
					}
					//コンパイル成功XXXXXXXXXXXXXXXXX
					else if(flag == true) {
						Label success = new Label();
						error_message.add(success);
						success.setText("コンパイル成功");
						success.setStyle("-fx-border-color: black;");
						err_Label.getChildren().add(success);
					}
					
					br.close();
/*メッセージ処理結果表示*/
					int i = 0;

					while(i < code1.length) {
						Label Code = new Label();
						int index = 0;
						int tab = 0;
						while(true) {
						    index = code1[i].indexOf("\t", index) + 1;
						    if (index == 0) break;
						    tab++;
						}
						Code.setText(code1[i].replace("\t", ""));
						Code.setId("code_display");
						Code.relocate(50 * tab, i * 20);
						//Code.setY(i * 15);
						if(error_line_num.indexOf(i+1)!=-1) {
								Code.setStyle("-fx-background-color: rgba(255,0,0,0.4);-fx-border-color: black;");
						}
						if(warning_line_num.indexOf(i+1)!=-1) {
								Code.setStyle("-fx-background-color: rgba(238,236,37,0.5);-fx-border-color: black;");
						}
						//borderPane.setLeft(Code);
						compiledCode.getChildren().add(Code);
						i++;
					}
				}catch(Exception ex) {
				}
			}catch(Exception ex){
			}
		});
	}

	class Stream extends Thread{
		private InputStream is;
		private BufferedOutputStream bos;
		private static final int BUFFER_SIZE = 32768;

		public Stream(InputStream is, String fileName) throws IOException{
			this.is = is;
			this.bos = new BufferedOutputStream(new FileOutputStream(fileName));
		}

		public void run() {
			byte[] buffer = new byte[BUFFER_SIZE];
			int flag = -1;

			try {
				while(true) {
					flag = is.read(buffer, 0, BUFFER_SIZE);
					if(flag == -1)break;
					bos.write(buffer, 0, flag);
				}
			}catch(IOException e) {
				e.printStackTrace();
			}finally {
				try {
					is.close();
				}catch(IOException e) {
				}
				try {
					bos.close();
				}catch(IOException e) {
				}
			}
		}
	}


	public static void main(String[] args) {
		launch(args);
	}
}

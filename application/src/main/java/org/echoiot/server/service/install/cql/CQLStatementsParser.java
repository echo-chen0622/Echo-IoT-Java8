package org.echoiot.server.service.install.cql;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CQL 语法解析器
 */
@Slf4j
public class CQLStatementsParser {

    enum State {
        // 语法内容，分为正常语法、注释、字符串等
        DEFAULT,
        INSINGLELINECOMMENT,
        INMULTILINECOMMENT,
        INQUOTESTRING,
        INSQUOTESTRING,

    }

    private final String text;
    private State state;
    private int pos;
    private List<String> statements;

    public CQLStatementsParser(Path cql) throws IOException {
        try {
            // 读取文件，并转成字符串cql
            List<String> lines = Files.readAllLines(cql);
            StringBuilder t = new StringBuilder();
            for (String l : lines) {
                t.append(l.trim());
                t.append('\n');
            }

            text = t.toString();
            pos = 0;
            //首先默认为是正常语法
            state = State.DEFAULT;
            // 执行解析过程
            parseStatements();
        } catch (IOException e) {
            log.error("无法分析 CQL 文件 [{}]!", cql);
            log.error("Exception", e);
            throw e;
        }
    }

    public List<String> getStatements() {
        return this.statements;
    }

    private void parseStatements() {
        this.statements = new ArrayList<>();
        StringBuilder statementUnderConstruction = new StringBuilder();

        char c;
        while ((c = getChar()) != 0) {
            switch (state) {
                case DEFAULT:
                    //语法解析
                    processDefaultState(c, statementUnderConstruction);
                    break;
                case INSINGLELINECOMMENT:
                    if (c == '\n') {
                        //这里表示单行注释结束
                        state = State.DEFAULT;
                    }
                    //注释不需要做任务处理，直接丢弃，直到发现结束标志
                    break;

                case INMULTILINECOMMENT:
                    if (c == '*' && peekAhead() == '/') {
                        //这里表示多行注释结束
                        state = State.DEFAULT;
                        advance();
                    }
                    //注释不需要做任务处理，直接丢弃，直到发现结束标志
                    break;

                case INQUOTESTRING:
                    // 这里表示字符串解析
                    processInQuoteStringState(c, statementUnderConstruction);
                    break;
                case INSQUOTESTRING:
                    // 这里表示字符串解析
                    processInSQuoteStringState(c, statementUnderConstruction);
                    break;
                default:
                    throw new RuntimeException("未知的语法类型: " + state);
            }

        }
        String tmp = statementUnderConstruction.toString().trim();
        if (tmp.length() > 0) {
            this.statements.add(tmp);
        }
    }

    /**
     * 逐字解析 cql语法。cql 语法比较简单，只有注释、字符串、语句等几种类型，这里只需要根据这几种类型进行解析即可。
     * 实际上，如果要做更复杂的语法解析器（比如说需要识别嵌套语法），是可以抽象出一个大型语法解析工具的。但是这里就不做延伸了，没有必要。
     *
     * @param c                          当前字符
     * @param statementUnderConstruction 当前语句
     */
    private void processDefaultState(char c, StringBuilder statementUnderConstruction) {
        if ((c == '/' && peekAhead() == '/') || (c == '-' && peekAhead() == '-')) {
            // 扫描到单行注释的开始符 （// or --） ，设置注释解析标记，并丢弃注释。终止符是换行符
            state = State.INSINGLELINECOMMENT;
            advance();
        } else if (c == '/' && peekAhead() == '*') {
            // 扫描到多行注释的开始符 （/*），设置注释解析标记，并丢弃注释。终止符是 */。
            state = State.INMULTILINECOMMENT;
            advance();
        } else if (c == '\n') {
            // 换行不是一条 cql 语句的结束符号，效用等同于空格
            statementUnderConstruction.append(' ');
        } else {
            // 可执行语句正常拼接
            statementUnderConstruction.append(c);
            if (c == '\"') {
                //发现字符串开始符，设置字符串解析标记，结束符是另一个双引号
                state = State.INQUOTESTRING;
            } else if (c == '\'') {
                //发现单引号字符串开始符，设置字符串解析标记，结束符是另一个单引号
                state = State.INSQUOTESTRING;
            } else if (c == ';') {
                //发现语句结束符，将语句添加到语句列表中
                statements.add(statementUnderConstruction.toString().trim());
                // 重置语句缓存
                statementUnderConstruction.setLength(0);
            }
        }
    }

    /**
     * 以双引号开始的字符串解析
     * 这个方法暂时没看出来有啥意义
     *
     * @param c
     * @param statementUnderConstruction
     */
    private void processInQuoteStringState(char c, @NotNull StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        if (c == '"') {
            //识别到结束符，判断下一个字符是否也是结束符，如果是，那么这个字符串中包含了一个结束符，否则，字符串解析结束。这里其实没有考虑字符串中有转义符/"的情况
            //个人认为结束符连续出现双引号的情况，在正经 cql 语法中应该是不存在的，是否需要这么处理尚且存疑
            if (peekAhead() == '"') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    /**
     * 以单引号开始的字符串解析
     * 这个方法暂时没看出来有啥意义
     *
     * @param c
     * @param statementUnderConstruction
     */
    private void processInSQuoteStringState(char c, @NotNull StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        //识别到结束符，判断下一个字符是否也是结束符，如果是，那么这个字符串中包含了一个结束符，否则，字符串解析结束
        //个人认为结束符连续出现单引号的情况，在正经 cql 语法中应该是不存在的，是否需要这么处理尚且存疑
        if (c == '\'') {
            if (peekAhead() == '\'') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    /**
     * 获取下一个字符，并前进一个位置
     */
    private char getChar() {
        if (pos < text.length()) {return text.charAt(pos++);} else {return 0;}
    }

    /**
     * 在不前进位置的情况下查看下一个字符。
     */
    private char peekAhead() {
        if (pos < text.length()) {
            return text.charAt(pos);  // don't advance
        } else {return 0;}
    }

    /**
     * 前进一个位置
     */
    private void advance() {
        pos++;
    }

}

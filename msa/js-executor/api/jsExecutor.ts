import vm, {Script} from 'vm';

export type TbScript = Script | Function;

export class JsExecutor {
    useSandbox: boolean;

    constructor(useSandbox: boolean) {
        this.useSandbox = useSandbox;
    }

    compileScript(code: string): Promise<TbScript> {
        if (this.useSandbox) {
            return this.createScript(code);
        } else {
            return this.createFunction(code);
        }
    }

    executeScript(script: TbScript, args: string[], timeout?: number): Promise<any> {
        if (this.useSandbox) {
            return this.invokeScript(script as Script, args, timeout);
        } else {
            return this.invokeFunction(script as Function, args);
        }
    }

    private createScript(code: string): Promise<Script> {
        return new Promise((resolve, reject) => {
            try {
                code = "("+code+")(...args)";
                const script = new vm.Script(code);
                resolve(script);
            } catch (err) {
                reject(err);
            }
        });
    }

    private invokeScript(script: Script, args: string[], timeout: number | undefined): Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                const sandbox = Object.create(null);
                sandbox.args = args;
                const result = script.runInNewContext(sandbox, {timeout: timeout});
                resolve(result);
            } catch (err) {
                reject(err);
            }
        });
    }


    private createFunction(code: string): Promise<Function> {
        return new Promise((resolve, reject) => {
            try {
                code = "return ("+code+")(...args)";
                const parsingContext = vm.createContext({});
                const func = vm.compileFunction(code, ['args'], {parsingContext: parsingContext});
                resolve(func);
            } catch (err) {
                reject(err);
            }
        });
    }

    private invokeFunction(func: Function, args: string[]): Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                const result = func(args);
                resolve(result);
            } catch (err) {
                reject(err);
            }
        });
    }
}

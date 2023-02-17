export interface IQueue {
    name: string;
    init(): Promise<void>;
    send(responseTopic: string, msgKey: string, rawResponse: Buffer, headers: any): Promise<any>;
    destroy(): Promise<void>;
}

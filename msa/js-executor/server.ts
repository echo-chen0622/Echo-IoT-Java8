import config from 'config';
import {_logger} from './config/logger';
import {HttpServer} from './api/httpServer';
import {IQueue} from './queue/queue.models';
import {KafkaTemplate} from './queue/kafkaTemplate';
import {PubSubTemplate} from './queue/pubSubTemplate';
import {AwsSqsTemplate} from './queue/awsSqsTemplate';
import {RabbitMqTemplate} from './queue/rabbitmqTemplate';
import {ServiceBusTemplate} from './queue/serviceBusTemplate';

const logger = _logger('main');

logger.info('===CONFIG BEGIN===');
logger.info(JSON.stringify(config, null, 4));
logger.info('===CONFIG END===');

const serviceType: string = config.get('queue_type');
const httpPort = Number(config.get('http_port'));
let queues: IQueue | null;
let httpServer: HttpServer | null;

(async () => {
    logger.info('Starting Echoiot JavaScript Executor Microservice...');
    try {
        queues = await createQueue(serviceType);
        logger.info(`Starting ${queues.name} template...`);
        await queues.init();
        logger.info(`${queues.name} template started.`);
        httpServer = new HttpServer(httpPort);
    } catch (e: any) {
        logger.error('Failed to start Echoiot JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        await exit(-1);
    }

})();

async function createQueue(serviceType: string): Promise<IQueue> {
    switch (serviceType) {
        case 'kafka':
            return new KafkaTemplate();
        case 'pubsub':
            return new PubSubTemplate();
        case 'aws-sqs':
            return new AwsSqsTemplate();
        case 'rabbitmq':
            return new RabbitMqTemplate();
        case 'service-bus':
            return new ServiceBusTemplate();
        default:
            throw new Error('Unknown service type: ' + serviceType);
    }
}

[`SIGINT`, `SIGUSR1`, `SIGUSR2`, `uncaughtException`, `SIGTERM`].forEach((eventType) => {
    process.once(eventType, async () => {
        logger.info(`${eventType} signal received`);
        await exit(0);
    })
})

process.on('exit', (code: number) => {
    logger.info(`Echoiot JavaScript Executor Microservice has been stopped. Exit code: ${code}.`);
});

async function exit(status: number) {
    logger.info('Exiting with status: %d ...', status);
    try {
        if (httpServer) {
            const _httpServer = httpServer;
            httpServer = null;
            await _httpServer.stop();
        }
        if (queues) {
            const _queues = queues;
            queues = null;
            await _queues.destroy();
        }
    } catch (e) {
        logger.error('Error on exit');
    }
    process.exit(status);
}
